package cars.student;

import cars.engine.Car;
import cars.engine.Vector2;
import cars.student.agentes.Agente;
import cars.student.agentes.Personalidade;
import cars.student.agentes.Sobrevivente;
import cars.student.agentes.Zumbi;
import cars.student.tela.Renderizador;

import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static cars.engine.Vector2.distance;

/**
 * Guarda o estado do jogo (mapa, agentes, itens, tiros) e aplica as regras
 * que não são de IA: dano, morte, respawn e reposição de itens.
 *
 * Ordem de cada frame (ver Window.update):
 *   1) a engine move cada agente com o steering calculado pelo estado dele;
 *   2) atualizar() aplica as regras do jogo.
 */
public class Simulacao {

    private static final int ITENS_NO_MAPA = 5;
    private static final double DISTANCIA_MINIMA_ENTRE_ITENS = 60;
    private static final double DISTANCIA_DO_ITEM_AO_SOBREVIVENTE = 150;
    private static final double DISTANCIA_DO_ZUMBI_AO_SOBREVIVENTE = 350;

    private static final double DISTANCIA_DA_MORDIDA = 2 * Agente.RAIO + 2;
    private static final double DANO_DA_MORDIDA_POR_SEGUNDO = 20;
    private static final double DANO_DO_TIRO = 25;

    private final Mapa mapa = new Mapa();
    private final Opcoes opcoes = new Opcoes();
    private final Teclado teclado = new Teclado();
    private final Renderizador renderizador = new Renderizador(this);

    private final Sobrevivente sobrevivente;
    private final List<Zumbi> zumbis = new ArrayList<>();
    private final List<Item> itens = new ArrayList<>();
    private final List<Tiro> tiros = new ArrayList<>();

    /** Pedidos do botão "Adicionar zumbi". Atômico porque vem da thread do Swing. */
    private final AtomicInteger zumbisPedidos = new AtomicInteger();
    /** Zumbi mostrado no painel "ao vivo": o do jogador, ou o mais próximo do sobrevivente. */
    private volatile Zumbi zumbiEmFoco = null;
    private int zumbisAbatidos = 0;
    private boolean terminou = false;

    public Simulacao(Personalidade personalidade) {
        sobrevivente = new Sobrevivente(this, mapa.posicaoLivreAleatoria(), personalidade);
        zumbis.add(new Zumbi(this, posicaoLongeDoSobrevivente(DISTANCIA_DO_ZUMBI_AO_SOBREVIVENTE), true));
        reporItens();
        escolherZumbiEmFoco();
    }

    /** Lista que a engine usa para mover todo mundo. */
    public List<Car> getCarros() {
        List<Car> carros = new ArrayList<>(zumbis);
        carros.add(sobrevivente);
        return carros;
    }

    // =====================================================================
    //  REGRAS DO JOGO (executadas depois do movimento)
    // =====================================================================

    public void atualizar(double secs) {
        moverTiros(secs);
        aplicarMordidas(secs);
        respawnarZumbisMortos();
        adicionarZumbisPedidos();
        reporItens();
        escolherZumbiEmFoco();

        if (sobrevivente.estaMorto()) {
            terminou = true;
            sobrevivente.printStats("MORREU. Fim da simulação.");
        }
    }

    private void moverTiros(double secs) {
        for (Tiro tiro : tiros) {
            tiro.mover(secs);
            if (mapa.ehObstaculo(tiro.getPosicao())) tiro.desativar();

            for (Zumbi zumbi : zumbis) {
                if (tiro.estaAtivo() && distance(tiro.getPosicao(), zumbi.getPosition()) < Agente.RAIO + 2) {
                    zumbi.receberDano(DANO_DO_TIRO);
                    tiro.desativar();
                }
            }
        }
        tiros.removeIf(tiro -> !tiro.estaAtivo());
    }

    private void aplicarMordidas(double secs) {
        for (Zumbi zumbi : zumbis) {
            if (distance(zumbi.getPosition(), sobrevivente.getPosition()) < DISTANCIA_DA_MORDIDA) {
                sobrevivente.receberDano(DANO_DA_MORDIDA_POR_SEGUNDO * secs);
            }
        }
    }

    /** Zumbi morto renasce em outro lugar do mapa, longe do sobrevivente. */
    private void respawnarZumbisMortos() {
        for (int i = 0; i < zumbis.size(); i++) {
            Zumbi zumbi = zumbis.get(i);
            if (zumbi.estaMorto()) {
                zumbi.printStats("Abatido! Renascendo em outro lugar.");
                zumbisAbatidos++;
                Vector2 novaPosicao = posicaoLongeDoSobrevivente(DISTANCIA_DO_ZUMBI_AO_SOBREVIVENTE);
                zumbis.set(i, new Zumbi(this, novaPosicao, zumbi.isDoJogador()));
            }
        }
    }

    private void adicionarZumbisPedidos() {
        int quantidade = zumbisPedidos.getAndSet(0);
        for (int i = 0; i < quantidade; i++) {
            zumbis.add(new Zumbi(this, posicaoLongeDoSobrevivente(DISTANCIA_DO_ZUMBI_AO_SOBREVIVENTE), false));
        }
    }

    /** Mantém sempre ITENS_NO_MAPA itens espalhados, longe uns dos outros e do sobrevivente. */
    private void reporItens() {
        while (itens.size() < ITENS_NO_MAPA) {
            itens.add(new Item(posicaoParaNovoItem()));
        }
    }

    private void escolherZumbiEmFoco() {
        Zumbi escolhido = null;
        for (Zumbi zumbi : zumbis) {
            if (zumbi.isDoJogador()) {
                escolhido = zumbi;
                break;
            }
            if (escolhido == null || distance(zumbi.getPosition(), sobrevivente.getPosition())
                    < distance(escolhido.getPosition(), sobrevivente.getPosition())) {
                escolhido = zumbi;
            }
        }
        zumbiEmFoco = escolhido;
    }

    // =====================================================================
    //  SPAWN
    // =====================================================================

    private Vector2 posicaoLongeDoSobrevivente(double distanciaMinima) {
        Vector2 posicao;
        do {
            posicao = mapa.posicaoLivreAleatoria();
        } while (distance(posicao, sobrevivente.getPosition()) < distanciaMinima);
        return posicao;
    }

    private Vector2 posicaoParaNovoItem() {
        Vector2 posicao;
        do {
            posicao = posicaoLongeDoSobrevivente(DISTANCIA_DO_ITEM_AO_SOBREVIVENTE);
        } while (temItemPerto(posicao));
        return posicao;
    }

    private boolean temItemPerto(Vector2 posicao) {
        for (Item item : itens) {
            if (distance(item.getPosicao(), posicao) < DISTANCIA_MINIMA_ENTRE_ITENS) return true;
        }
        return false;
    }

    // =====================================================================
    //  AÇÕES PEDIDAS PELOS AGENTES E PELA JANELA DE DEBUG
    // =====================================================================

    public void dispararTiro(Vector2 origem, Vector2 alvo) {
        tiros.add(new Tiro(origem, alvo));
    }

    public void removerItem(Item item) {
        itens.remove(item);
    }

    /** Chamado pelo botão da janela de debug (outra thread): só registra o pedido. */
    public void pedirNovoZumbi() {
        zumbisPedidos.incrementAndGet();
    }

    public void desenhar(Graphics2D g, int larguraDaJanela, int alturaDaJanela) {
        renderizador.desenhar(g, larguraDaJanela, alturaDaJanela);
    }

    // =====================================================================
    //  GETTERS
    // =====================================================================

    public Mapa getMapa() { return mapa; }
    public Opcoes getOpcoes() { return opcoes; }
    public Teclado getTeclado() { return teclado; }
    public Sobrevivente getSobrevivente() { return sobrevivente; }
    public List<Zumbi> getZumbis() { return zumbis; }
    public List<Item> getItens() { return itens; }
    public List<Tiro> getTiros() { return tiros; }
    public Zumbi getZumbiEmFoco() { return zumbiEmFoco; }
    public int getZumbisAbatidos() { return zumbisAbatidos; }
    public boolean terminou() { return terminou; }
}

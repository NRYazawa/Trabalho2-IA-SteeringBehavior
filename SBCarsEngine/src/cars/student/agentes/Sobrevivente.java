package cars.student.agentes;

import cars.engine.Vector2;
import cars.engine.World;
import cars.student.Item;
import cars.student.Simulacao;
import cars.student.estados.sobrevivente.EstadoProcurando;
import cars.student.fsm.Character;
import cars.student.fsm.State;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;


/**
 * O personagem controlado pela IA. Procura itens, atira nos zumbis e foge
 * quando o perigo fica grande demais. A personalidade define os pesos.
 */
public class Sobrevivente extends Agente implements Character<Sobrevivente> {

    public static final double VELOCIDADE_MAXIMA = 80;
    public static final double FORCA_MAXIMA = 160;
    public static final double VIDA_MAXIMA = 100;

    /** Primeiro range: até onde ele enxerga e consegue atirar. */
    public static final double RAIO_DE_VISAO = 200;
    /** Ouvir atravessa paredes, mas só serve para levar um susto e olhar; para reagir ele precisa VER. */
    public static final double RAIO_DE_AUDICAO = 120;

    private static final double SEGUNDOS_ENTRE_TIROS = 0.55;
    private static final double ERRO_DA_MIRA = Math.toRadians(7); // cada tiro sai até 7° torto
    private static final double SEGUNDOS_DE_ALERTA = 1.0;          // tempo que o "!" fica na tela

    // Ponto de fuga: chão aberto que ele vê e onde chega antes do zumbi
    private static final double DISTANCIA_MINIMA_DA_FUGA = 60;
    private static final double DISTANCIA_MAXIMA_DA_FUGA = 260;

    private final Personalidade personalidade;
    private volatile State<Sobrevivente> estado; // volatile: a janela de Debug lê o nome do estado
    private int itensNaBolsa = 0;
    private double recargaDoTiro = 0;

    // O que ele percebeu neste frame (atualizado uma vez por frame, em perceber())
    private Zumbi zumbiVisto = null;
    private Zumbi zumbiOuvido = null;
    private Item itemVisto = null;

    // Cópia das vontades do último frame para a janela de Debug (lida por outra thread)
    private volatile Map<Intencao, Double> ultimasVontades = Map.of();
    private volatile Intencao ultimaDecisao = Intencao.LOOTEAR;

    // Alerta ("!" na cabeça): 0 = nada, 1 = ouviu, 2 = viu
    private int nivelDeAlertaAnterior = 0;
    private double tempoDeAlerta = 0;

    public Sobrevivente(Simulacao simulacao, Vector2 posicao, Personalidade personalidade) {
        super(simulacao, posicao, personalidade.getCor(), VELOCIDADE_MAXIMA, FORCA_MAXIMA, VIDA_MAXIMA);
        this.personalidade = personalidade;
        setState(new EstadoProcurando(this));
    }

    @Override
    protected void executarEstado(World world) {
        recargaDoTiro -= world.getSecs();
        tempoDeAlerta -= world.getSecs();
        perceber();
        guardarVontadesParaODebug();
        estado.execute(world);
    }

    // =====================================================================
    //  MÁQUINA DE ESTADOS
    // =====================================================================

    @Override
    public void setState(State<Sobrevivente> novoEstado) {
        if (estado != null) estado.leave();
        estado = novoEstado;
        printStats("-> " + novoEstado.getNome());
        estado.enter();
    }

    @Override
    public State<Sobrevivente> getState() {
        return estado;
    }

    @Override
    public String getNomeDoEstado() {
        return estado.getNome();
    }

    @Override
    public void printStats(String mensagem) {
        System.out.printf("[Sobrevivente - %s] %s | vida %.0f | itens %d%n",
                personalidade.getNome(), mensagem, getVida(), itensNaBolsa);
    }

    // =====================================================================
    //  PERCEPÇÃO
    // =====================================================================

    /** Primeiro passo de todo frame: olha e escuta em volta e guarda o que percebeu. */
    private void perceber() {
        zumbiVisto = zumbiMaisProximoVisivel(RAIO_DE_VISAO);
        zumbiOuvido = zumbiMaisProximoAte(RAIO_DE_AUDICAO); // ouvir atravessa paredes
        itemVisto = itemMaisProximoVisivel();

        // Mostra o "!" quando a percepção aumenta: nada -> ouviu, ou ouviu -> viu
        int nivelDeAlerta = zumbiVisto != null ? 2 : zumbiOuvido != null ? 1 : 0;
        if (nivelDeAlerta > nivelDeAlertaAnterior) tempoDeAlerta = SEGUNDOS_DE_ALERTA;
        nivelDeAlertaAnterior = nivelDeAlerta;
    }

    /** O rosto aponta para o zumbi que ele vê (ou ouve); sem zumbi, para onde está andando. */
    @Override
    public Vector2 getDirecaoDoOlhar() {
        Zumbi foco = zumbiVisto != null ? zumbiVisto : zumbiOuvido;
        if (foco == null) return super.getDirecaoDoOlhar();
        return Vector2.normalize(Vector2.subtract(foco.getPosition(), getPosition()));
    }

    /** O "!" está aparecendo? */
    public boolean estaEmAlerta() {
        return tempoDeAlerta > 0;
    }

    /** O "!" é por ter VISTO um zumbi (e não só ouvido)? */
    public boolean alertaFoiPorVisao() {
        return zumbiVisto != null;
    }

    /** Zumbi mais próximo à vista neste frame (ou null). */
    public Zumbi getZumbiVisto() {
        return zumbiVisto;
    }

    /** Item mais próximo à vista neste frame (ou null). */
    public Item getItemVisto() {
        return itemVisto;
    }

    /** Todos os zumbis que ele consegue ver até essa distância. */
    public List<Zumbi> zumbisVisiveis(double raio) {
        List<Zumbi> visiveis = new ArrayList<>();
        for (Zumbi zumbi : simulacao.getZumbis()) {
            if (consegueVer(zumbi.getPosition(), raio)) visiveis.add(zumbi);
        }
        return visiveis;
    }

    /** O zumbi mais próximo até essa distância, mesmo atrás de parede (audição). */
    private Zumbi zumbiMaisProximoAte(double raio) {
        Zumbi maisProximo = null;
        for (Zumbi zumbi : simulacao.getZumbis()) {
            double distancia = distanciaAte(zumbi.getPosition());
            if (distancia <= raio && (maisProximo == null || distancia < distanciaAte(maisProximo.getPosition()))) {
                maisProximo = zumbi;
            }
        }
        return maisProximo;
    }

    /** O zumbi visível mais próximo, ou null se não vê nenhum. */
    public Zumbi zumbiMaisProximoVisivel(double raio) {
        Zumbi maisProximo = null;
        for (Zumbi zumbi : zumbisVisiveis(raio)) {
            if (maisProximo == null || distanciaAte(zumbi.getPosition()) < distanciaAte(maisProximo.getPosition())) {
                maisProximo = zumbi;
            }
        }
        return maisProximo;
    }

    /** O item visível mais próximo, ou null se não vê nenhum. */
    private Item itemMaisProximoVisivel() {
        Item maisProximo = null;
        for (Item item : simulacao.getItens()) {
            if (!consegueVer(item.getPosicao(), RAIO_DE_VISAO)) continue;
            if (maisProximo == null || distanciaAte(item.getPosicao()) < distanciaAte(maisProximo.getPosicao())) {
                maisProximo = item;
            }
        }
        return maisProximo;
    }

    public double distanciaAte(Vector2 ponto) {
        return Vector2.distance(getPosition(), ponto);
    }

    // =====================================================================
    //  DECISÃO: as vontades (peso da personalidade × estímulo) disputam
    // =====================================================================

    /**
     * Perigo de 0 a 1: 0 quando o zumbi está na borda da visão (ou nem é visto),
     * 1 quando ele chega na distância mínima. Quanto mais perto, maior.
     */
    public double perigo() {
        if (zumbiVisto == null) return 0;

        double distancia = distanciaAte(zumbiVisto.getPosition());
        double perigo = (RAIO_DE_VISAO - distancia) / (RAIO_DE_VISAO - personalidade.getDistanciaMinima());
        return Math.clamp(perigo, 0.0, 1.0);
    }

    /**
     * Vontade = peso da personalidade × estímulo do momento.
     *   Lutar:   1 se vê um zumbi.
     *   Fugir:   o perigo (cresce conforme o zumbi chega perto).
     *   Lootear: 1 se vê um item, mas cai com o perigo (ninguém revira gaveta com zumbi do lado).
     */
    public double vontade(Intencao intencao) {
        double estimulo = switch (intencao) {
            case LUTAR -> zumbiVisto != null ? 1 : 0;
            case FUGIR -> perigo();
            case LOOTEAR -> itemVisto != null ? 1 - perigo() : 0;
        };
        return personalidade.getPeso(intencao) * estimulo;
    }

    /**
     * Ganha a maior vontade. As intenções são percorridas na ordem de ênfase
     * da personalidade e só trocamos se for MAIOR, então no empate fica a que
     * veio primeiro, ou seja, a ênfase do personagem.
     * Se nada estimula (tudo zero), ele segue procurando itens.
     */
    public Intencao decidirIntencao() {
        Intencao escolhida = Intencao.LOOTEAR;
        double maiorVontade = 0;
        for (Intencao intencao : personalidade.getOrdemDeEnfase()) {
            double vontade = vontade(intencao);
            if (vontade > maiorVontade) {
                maiorVontade = vontade;
                escolhida = intencao;
            }
        }
        return escolhida;
    }

    private void guardarVontadesParaODebug() {
        Map<Intencao, Double> vontades = new EnumMap<>(Intencao.class);
        for (Intencao intencao : Intencao.values()) {
            vontades.put(intencao, vontade(intencao));
        }
        ultimasVontades = Collections.unmodifiableMap(vontades);
        ultimaDecisao = decidirIntencao();
    }

    public Map<Intencao, Double> getUltimasVontades() { return ultimasVontades; }
    public Intencao getUltimaDecisao() { return ultimaDecisao; }

    // =====================================================================
    //  AÇÕES
    // =====================================================================

    /** Atira se a arma já recarregou. A mira não é perfeita: o tiro sai um pouco torto. */
    public void atirarEm(Zumbi alvo) {
        if (recargaDoTiro > 0) return;

        Vector2 direcao = Vector2.subtract(alvo.getPosition(), getPosition());
        double erro = (Math.random() * 2 - 1) * ERRO_DA_MIRA;
        Vector2 pontoMirado = Vector2.add(getPosition(), Vector2.rotate(direcao, erro));

        simulacao.dispararTiro(getPosition(), pontoMirado);
        recargaDoTiro = SEGUNDOS_ENTRE_TIROS;
    }

    /**
     * Rota de fuga: entre os pontos de chão aberto aonde ele consegue ir em linha reta
     * (o corpo inteiro passa, não só o olhar), escolhe um onde chega
     * ANTES do zumbi mais próximo (folga positiva). Entre esses, prefere mais folga e
     * pontos mais longe (corridas longas, sem ficar trocando de direção).
     * Devolve null se não existe nenhum: ele está encurralado.
     */
    public Vector2 escolherPontoDeFuga(List<Zumbi> ameacas) {
        Vector2 melhorPonto = null;
        double melhorNota = 0;
        for (Vector2 ponto : simulacao.getMapa().pontosAlcancaveis(getPosition(), DISTANCIA_MINIMA_DA_FUGA, DISTANCIA_MAXIMA_DA_FUGA, RAIO)) {
            double folga = distanciaDoZumbiMaisProximo(ponto, ameacas) - distanciaAte(ponto);
            if (folga <= 0) continue; // o zumbi chegaria antes: rota ruim

            double nota = folga + 0.5 * distanciaAte(ponto);
            if (nota > melhorNota) {
                melhorNota = nota;
                melhorPonto = ponto;
            }
        }
        return melhorPonto;
    }

    private static double distanciaDoZumbiMaisProximo(Vector2 ponto, List<Zumbi> zumbis) {
        double menor = Double.POSITIVE_INFINITY;
        for (Zumbi zumbi : zumbis) {
            menor = Math.min(menor, Vector2.distance(ponto, zumbi.getPosition()));
        }
        return menor;
    }

    public void guardarNaBolsa(Item item) {
        simulacao.removerItem(item);
        itensNaBolsa++;
        printStats("Guardou na bolsa: " + item.getNome());
    }

    public Personalidade getPersonalidade() { return personalidade; }
    public int getItensNaBolsa() { return itensNaBolsa; }
}

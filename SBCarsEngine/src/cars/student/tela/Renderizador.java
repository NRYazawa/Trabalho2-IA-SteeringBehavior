package cars.student.tela;

import cars.engine.Vector2;
import cars.student.Caminho;
import cars.student.Item;
import cars.student.Mapa;
import cars.student.Opcoes;
import cars.student.Simulacao;
import cars.student.Tiro;
import cars.student.agentes.*;
import cars.student.estados.sobrevivente.EstadoFugindo;
import cars.student.estados.sobrevivente.EstadoInvestigando;
import cars.student.estados.sobrevivente.EstadoProcurando;
import cars.student.estados.zumbi.EstadoPatrulhando;

import java.awt.*;
import java.awt.geom.*;
import java.util.Map;

import static cars.engine.Vector2.*;

/**
 * Desenha tudo: mapa, rota de patrulha, itens, tiros, agentes, debug e HUD.
 * Só lê a simulação, nunca altera nada nela.
 * Coordenadas: (0,0) é o centro da janela, igual ao mundo da engine.
 */
public class Renderizador {

    public static final Color COR_RESULTANTE = new Color(20, 20, 20);
    public static final Color COR_VELOCIDADE = new Color(40, 90, 255);

    private static final Color COR_FUNDO = new Color(43, 45, 49);
    private static final Color COR_CHAO = new Color(232, 228, 218);
    private static final Color COR_REJUNTE = new Color(220, 215, 204);
    private static final Color COR_PAREDE = new Color(60, 64, 72);
    private static final Color COR_MOVEL = new Color(150, 110, 70);
    private static final Color COR_ITEM = new Color(240, 190, 30);
    private static final Color COR_TIRO = new Color(255, 120, 0);
    private static final Color COR_VISAO = new Color(255, 200, 0);
    private static final Color COR_ROTA = Comportamento.PATH_FOLLOWING.getCor();
    private static final Color COR_ANTENA_LIVRE = new Color(90, 90, 90, 150);
    private static final Color COR_ANTENA_BATEU = new Color(230, 30, 30);

    private static final double ESCALA_DAS_FORCAS = 0.35;
    private static final double ESCALA_DA_VELOCIDADE = 0.6;

    // Faixas fora do mapa usadas pelo HUD
    private static final double TOPO_DO_MAPA = -Mapa.ALTURA / 2.0;
    private static final double BASE_DO_MAPA = Mapa.ALTURA / 2.0;
    private static final double ESQUERDA_DO_MAPA = -Mapa.LARGURA / 2.0;
    private static final double DIREITA_DO_MAPA = Mapa.LARGURA / 2.0;

    private final Simulacao simulacao;

    public Renderizador(Simulacao simulacao) {
        this.simulacao = simulacao;
    }

    public void desenhar(Graphics2D g, int largura, int altura) {
        Opcoes opcoes = simulacao.getOpcoes();
        Sobrevivente sobrevivente = simulacao.getSobrevivente();

        g.setColor(COR_FUNDO);
        g.fill(new Rectangle2D.Double(-largura / 2.0, -altura / 2.0, largura, altura));

        // Mundo (tudo recortado no retângulo do mapa, para não invadir o HUD)
        Shape recorteOriginal = g.getClip();
        g.clip(new Rectangle2D.Double(ESQUERDA_DO_MAPA, TOPO_DO_MAPA, Mapa.LARGURA, Mapa.ALTURA));
        desenharMapa(g);
        if (opcoes.mostrarRota) desenharRota(g);
        for (Item item : simulacao.getItens()) desenharItem(g, item);
        for (Tiro tiro : simulacao.getTiros()) desenharTiro(g, tiro);
        for (Zumbi zumbi : simulacao.getZumbis()) desenharAgente(g, zumbi);
        desenharAgente(g, sobrevivente);
        desenharProgressoDaInvestigacao(g, sobrevivente);

        // O que o sobrevivente realmente enxerga (paredes fazem sombra)
        Shape areaVisivel = simulacao.getMapa().areaVisivel(sobrevivente.getPosition(), Sobrevivente.RAIO_DE_VISAO);
        if (opcoes.modo == Opcoes.ModoDeJogo.SOBREVIVENTE) desenharEscuridao(g, sobrevivente, areaVisivel);
        desenharAlerta(g, sobrevivente);

        // Debug (por cima da escuridão, para continuar visível)
        if (opcoes.mostrarVisao) desenharRaios(g, sobrevivente, areaVisivel);
        if (opcoes.mostrarRota) {
            for (Zumbi zumbi : simulacao.getZumbis()) desenharSeguimento(g, zumbi);
        }
        if (opcoes.mostrarAntenas) {
            for (Zumbi zumbi : simulacao.getZumbis()) desenharAntenas(g, zumbi);
            desenharAntenas(g, sobrevivente);
        }
        if (opcoes.mostrarForcas) {
            for (Zumbi zumbi : simulacao.getZumbis()) desenharForcas(g, zumbi);
            desenharForcas(g, sobrevivente);
            desenharDestino(g, sobrevivente);
        }
        if (opcoes.mostrarEstados) {
            for (Zumbi zumbi : simulacao.getZumbis()) desenharNomeDoEstado(g, zumbi);
            desenharNomeDoEstado(g, sobrevivente);
        }

        g.setClip(recorteOriginal);

        // Interface
        if (opcoes.mostrarEstados) desenharVontades(g, sobrevivente);
        desenharHud(g, sobrevivente, opcoes);
        if (simulacao.terminou()) desenharFimDeJogo(g, sobrevivente, largura, altura);
    }

    // =====================================================================
    //  MUNDO
    // =====================================================================

    private void desenharMapa(Graphics2D g) {
        g.setColor(COR_CHAO);
        g.fill(new Rectangle2D.Double(ESQUERDA_DO_MAPA, TOPO_DO_MAPA, Mapa.LARGURA, Mapa.ALTURA));

        g.setColor(COR_REJUNTE); // linhas do piso, só para parecer chão de escritório
        g.setStroke(new BasicStroke(1f));
        for (double x = ESQUERDA_DO_MAPA; x < DIREITA_DO_MAPA; x += Mapa.TAMANHO_DA_CELULA) {
            g.draw(new Line2D.Double(x, TOPO_DO_MAPA, x, BASE_DO_MAPA));
        }
        for (double y = TOPO_DO_MAPA; y < BASE_DO_MAPA; y += Mapa.TAMANHO_DA_CELULA) {
            g.draw(new Line2D.Double(ESQUERDA_DO_MAPA, y, DIREITA_DO_MAPA, y));
        }

        g.setColor(COR_MOVEL);
        for (Rectangle2D movel : simulacao.getMapa().getMoveis()) {
            g.fill(new Rectangle2D.Double(movel.getX() + 2, movel.getY() + 2, movel.getWidth() - 4, movel.getHeight() - 4));
        }

        g.setColor(COR_PAREDE);
        for (Rectangle2D parede : simulacao.getMapa().getParedes()) g.fill(parede);
    }

    private void desenharItem(Graphics2D g, Item item) {
        Vector2 p = item.getPosicao();
        Rectangle2D caixa = new Rectangle2D.Double(p.x - 6, p.y - 6, 12, 12);
        g.setColor(COR_ITEM);
        g.fill(caixa);
        g.setColor(COR_ITEM.darker().darker());
        g.setStroke(new BasicStroke(1.5f));
        g.draw(caixa);
    }

    private void desenharTiro(Graphics2D g, Tiro tiro) {
        Vector2 ponta = tiro.getPosicao();
        Vector2 cauda = subtract(ponta, multiply(tiro.getVelocidade(), 0.025));
        g.setColor(COR_TIRO);
        g.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new Line2D.Double(cauda.x, cauda.y, ponta.x, ponta.y));
    }

    /** O agente é um quadradinho girado para onde está olhando, com "olhos" na frente. */
    private void desenharAgente(Graphics2D g, Agente agente) {
        Vector2 p = agente.getPosition();
        Vector2 frente = agente.getDirecaoDoOlhar();
        double r = Agente.RAIO;

        AffineTransform original = g.getTransform();
        g.translate(p.x, p.y);
        g.rotate(Math.atan2(frente.y, frente.x));

        Rectangle2D corpo = new Rectangle2D.Double(-r, -r, 2 * r, 2 * r);
        g.setColor(agente.getCor());
        g.fill(corpo);
        boolean doJogador = agente instanceof Zumbi zumbi && zumbi.isDoJogador();
        g.setColor(doJogador ? Color.WHITE : agente.getCor().darker().darker());
        g.setStroke(new BasicStroke(doJogador ? 2.5f : 1.5f));
        g.draw(corpo);

        g.setColor(Color.BLACK); // olhos
        g.fill(new Rectangle2D.Double(r - 5, -r + 3, 3, 4));
        g.fill(new Rectangle2D.Double(r - 5, r - 7, 3, 4));
        g.setTransform(original);

        if (agente instanceof Zumbi) desenharBarra(g, p.x - 12, p.y - r - 8, 24, 4,
                agente.getVida() / agente.getVidaMaxima(), new Color(200, 40, 40));
    }

    private void desenharProgressoDaInvestigacao(Graphics2D g, Sobrevivente sobrevivente) {
        if (sobrevivente.getState() instanceof EstadoInvestigando investigando) {
            Vector2 p = sobrevivente.getPosition();
            desenharBarra(g, p.x - 15, p.y - Agente.RAIO - 10, 30, 5, investigando.getProgresso(), COR_ITEM);
        }
    }

    /**
     * Escuro em tudo; só a área que o sobrevivente realmente vê fica clara,
     * escurecendo aos poucos até a borda (efeito de lanterna).
     */
    private void desenharEscuridao(Graphics2D g, Sobrevivente sobrevivente, Shape areaVisivel) {
        Rectangle2D mapa = new Rectangle2D.Double(ESQUERDA_DO_MAPA, TOPO_DO_MAPA, Mapa.LARGURA, Mapa.ALTURA);

        // Fora da área visível: breu
        Area escuro = new Area(mapa);
        escuro.subtract(new Area(areaVisivel));
        g.setColor(new Color(0, 0, 0, 250));
        g.fill(escuro);

        // Dentro da área visível: claro no centro, escurecendo até a borda
        Vector2 p = sobrevivente.getPosition();
        RadialGradientPaint lanterna = new RadialGradientPaint(
                new Point2D.Double(p.x, p.y), (float) Sobrevivente.RAIO_DE_VISAO,
                new float[]{0f, 0.7f, 1f},
                new Color[]{new Color(0, 0, 0, 0), new Color(0, 0, 0, 120), new Color(0, 0, 0, 250)});
        g.setPaint(lanterna);
        g.fill(areaVisivel);
    }

    /** "!" em cima da cabeça: amarelo quando ouviu um zumbi, vermelho quando viu. */
    private void desenharAlerta(Graphics2D g, Sobrevivente sobrevivente) {
        if (!sobrevivente.estaEmAlerta()) return;

        Vector2 p = sobrevivente.getPosition();
        Color cor = sobrevivente.alertaFoiPorVisao() ? new Color(220, 30, 30) : new Color(240, 180, 0);
        double y = p.y - Agente.RAIO - 24;

        g.setColor(Color.WHITE);
        g.fill(new Ellipse2D.Double(p.x - 9, y - 9, 18, 18));
        g.setColor(cor);
        g.setStroke(new BasicStroke(2f));
        g.draw(new Ellipse2D.Double(p.x - 9, y - 9, 18, 18));
        g.setFont(new Font("SansSerif", Font.BOLD, 15));
        g.drawString("!", (float) (p.x - 2.5), (float) (y + 5.5));
    }

    // =====================================================================
    //  DEBUG
    // =====================================================================

    private void desenharRaios(Graphics2D g, Sobrevivente sobrevivente, Shape areaVisivel) {
        Personalidade personalidade = sobrevivente.getPersonalidade();
        Vector2 p = sobrevivente.getPosition();

        // Zumbis: raio de visão bem clarinho
        for (Zumbi zumbi : simulacao.getZumbis()) {
            desenharCirculo(g, zumbi.getPosition(), Zumbi.RAIO_DE_VISAO, new Color(200, 40, 40, 70), false);
        }

        // Sobrevivente: área que ele realmente vê (preenchida), alcance da visão/tiro,
        // audição, distância mínima (laranja) e distância segura (cinza)
        g.setColor(new Color(255, 200, 0, 45));
        g.fill(areaVisivel);
        desenharCirculo(g, p, Sobrevivente.RAIO_DE_VISAO, COR_VISAO, false);
        desenharCirculo(g, p, Sobrevivente.RAIO_DE_AUDICAO, new Color(255, 255, 255, 140), true);
        desenharCirculo(g, p, personalidade.getDistanciaMinima(), new Color(230, 90, 0), true);
        desenharCirculo(g, p, personalidade.getDistanciaSegura(), new Color(120, 120, 120, 120), true);
    }

    /**
     * Rota de patrulha (Path Following): o "tubo" (largura = 2 × raio) e a linha central,
     * com setinhas mostrando o sentido.
     */
    private void desenharRota(Graphics2D g) {
        Caminho rota = simulacao.getMapa().getRotaDePatrulha();
        Path2D linha = new Path2D.Double();
        for (int i = 0; i < rota.quantidadeDeSegmentos(); i++) {
            Vector2 inicio = rota.inicioDoSegmento(i);
            if (i == 0) linha.moveTo(inicio.x, inicio.y); else linha.lineTo(inicio.x, inicio.y);
        }
        linha.closePath();

        g.setColor(new Color(COR_ROTA.getRed(), COR_ROTA.getGreen(), COR_ROTA.getBlue(), 40));
        g.setStroke(new BasicStroke((float) (2 * rota.getRaio()), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(linha);
        g.setColor(new Color(COR_ROTA.getRed(), COR_ROTA.getGreen(), COR_ROTA.getBlue(), 150));
        g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 10f, new float[]{8f, 6f}, 0f));
        g.draw(linha);

        for (int i = 0; i < rota.quantidadeDeSegmentos(); i++) {
            Vector2 meio = multiply(add(rota.inicioDoSegmento(i), rota.fimDoSegmento(i)), 0.5);
            Vector2 sentido = multiply(rota.direcaoDoSegmento(i), 10);
            desenharSeta(g, subtract(meio, sentido), multiply(sentido, 2), COR_ROTA, 1.5f);
        }
    }

    /**
     * O raciocínio do Path Following de um zumbi patrulhando:
     * posição futura (círculo), onde ela cai na rota (linha até a projeção) e o alvo adiante (X).
     * A linha fica vermelha quando a posição futura sai do tubo (aí ele usa Seek para voltar).
     */
    private void desenharSeguimento(Graphics2D g, Zumbi zumbi) {
        if (!(zumbi.getState() instanceof EstadoPatrulhando patrulhando)) return;
        Agente.Seguimento seguimento = patrulhando.getUltimoSeguimento();
        if (seguimento == null) return;

        Vector2 p = zumbi.getPosition();
        Vector2 futuro = seguimento.futuro();
        Vector2 projecao = seguimento.projecao();
        Vector2 alvo = seguimento.alvo();
        Color cor = seguimento.dentroDoCaminho() ? COR_ROTA : COR_ANTENA_BATEU;

        g.setStroke(new BasicStroke(1f));
        g.setColor(cor);
        g.draw(new Line2D.Double(p.x, p.y, futuro.x, futuro.y));
        g.draw(new Ellipse2D.Double(futuro.x - 3, futuro.y - 3, 6, 6));
        g.draw(new Line2D.Double(futuro.x, futuro.y, projecao.x, projecao.y));
        g.fill(new Ellipse2D.Double(projecao.x - 2, projecao.y - 2, 4, 4));
        g.setStroke(new BasicStroke(2f));
        g.draw(new Line2D.Double(alvo.x - 4, alvo.y - 4, alvo.x + 4, alvo.y + 4));
        g.draw(new Line2D.Double(alvo.x - 4, alvo.y + 4, alvo.x + 4, alvo.y - 4));
    }

    /**
     * Antenas do Obstacle Avoidance: cinza quando livres; vermelhas até onde bateram,
     * com um pontinho no ponto de impacto e a normal da parede.
     */
    private void desenharAntenas(Graphics2D g, Agente agente) {
        g.setStroke(new BasicStroke(1.2f));
        for (Agente.Antena antena : agente.getAntenas()) {
            Vector2 inicio = antena.inicio();
            if (antena.colisao() == null) {
                g.setColor(COR_ANTENA_LIVRE);
                g.draw(new Line2D.Double(inicio.x, inicio.y, antena.fim().x, antena.fim().y));
            } else {
                Vector2 impacto = antena.colisao().ponto();
                g.setColor(COR_ANTENA_LIVRE);
                g.draw(new Line2D.Double(impacto.x, impacto.y, antena.fim().x, antena.fim().y));
                g.setColor(COR_ANTENA_BATEU);
                g.draw(new Line2D.Double(inicio.x, inicio.y, impacto.x, impacto.y));
                g.fill(new Ellipse2D.Double(impacto.x - 2.5, impacto.y - 2.5, 5, 5));
                desenharSeta(g, impacto, multiply(antena.colisao().normal(), 12), COR_ANTENA_BATEU, 1.2f);
            }
        }
    }

    private void desenharForcas(Graphics2D g, Agente agente) {
        Vector2 origem = agente.getPosition();
        for (Map.Entry<Comportamento, Vector2> forca : agente.getForcas().entrySet()) {
            desenharSeta(g, origem, multiply(forca.getValue(), ESCALA_DAS_FORCAS), forca.getKey().getCor(), 2f);
        }
        desenharSeta(g, origem, multiply(agente.getResultante(), ESCALA_DAS_FORCAS), COR_RESULTANTE, 3f);
        desenharSeta(g, origem, multiply(agente.getVelocity(), ESCALA_DA_VELOCIDADE), COR_VELOCIDADE, 1.5f);
    }

    /** Destino escolhido pelo sobrevivente (exploração ou fuga): um X ligado a ele por uma linha. */
    private void desenharDestino(Graphics2D g, Sobrevivente sobrevivente) {
        Vector2 destino = null;
        if (sobrevivente.getState() instanceof EstadoProcurando procurando) destino = procurando.getDestino();
        if (sobrevivente.getState() instanceof EstadoFugindo fugindo) destino = fugindo.getPontoDeFuga();
        if (destino == null) return;

        Vector2 p = sobrevivente.getPosition();
        g.setColor(Comportamento.SEEK.getCor());
        g.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[]{4f, 4f}, 0f));
        g.draw(new Line2D.Double(p.x, p.y, destino.x, destino.y));
        g.setStroke(new BasicStroke(2f));
        g.draw(new Line2D.Double(destino.x - 5, destino.y - 5, destino.x + 5, destino.y + 5));
        g.draw(new Line2D.Double(destino.x - 5, destino.y + 5, destino.x + 5, destino.y - 5));
    }

    private void desenharNomeDoEstado(Graphics2D g, Agente agente) {
        Vector2 p = agente.getPosition();
        g.setFont(new Font("SansSerif", Font.BOLD, 11));
        String texto = agente.getNomeDoEstado();
        int larguraDoTexto = g.getFontMetrics().stringWidth(texto);
        float x = (float) (p.x - larguraDoTexto / 2.0);
        float y = (float) (p.y + Agente.RAIO + 14);

        g.setColor(new Color(255, 255, 255, 190));
        g.fill(new RoundRectangle2D.Double(x - 3, y - 11, larguraDoTexto + 6, 14, 6, 6));
        g.setColor(Color.BLACK);
        g.drawString(texto, x, y);
    }

    /** Barra de cada vontade embaixo do mapa, e qual delas venceu. */
    private void desenharVontades(Graphics2D g, Sobrevivente sobrevivente) {
        double y = BASE_DO_MAPA + 12;
        double x = ESQUERDA_DO_MAPA;
        g.setFont(new Font("SansSerif", Font.BOLD, 12));

        g.setColor(Color.WHITE);
        g.drawString("Vontades:", (float) x, (float) (y + 10));
        x += 75;

        for (Intencao intencao : Intencao.values()) {
            double vontade = sobrevivente.vontade(intencao);
            g.setColor(Color.WHITE);
            g.drawString(intencao.getNome(), (float) x, (float) (y + 10));
            desenharBarra(g, x + 55, y + 1, 100, 10, vontade, new Color(255, 200, 0));
            g.setColor(Color.WHITE);
            g.drawString(String.format("%.2f", vontade), (float) (x + 160), (float) (y + 10));
            x += 210;
        }

        g.setColor(new Color(255, 200, 0));
        g.drawString("Decisão: " + sobrevivente.decidirIntencao().getNome()
                + String.format("  (perigo %.0f%%)", sobrevivente.perigo() * 100), (float) x, (float) (y + 10));
    }

    // =====================================================================
    //  HUD E FIM DE JOGO
    // =====================================================================

    private void desenharHud(Graphics2D g, Sobrevivente sobrevivente, Opcoes opcoes) {
        float yTexto = (float) (TOPO_DO_MAPA - 16);
        g.setFont(new Font("SansSerif", Font.BOLD, 14));

        // Canto superior esquerdo: modo e personagem
        String modo = opcoes.modo == Opcoes.ModoDeJogo.TESTE ? "Modo Teste" : "Modo Sobrevivente";
        g.setColor(Color.WHITE);
        g.drawString(String.format("%s  |  %s  |  Zumbis: %d  |  Abatidos: %d", modo,
                sobrevivente.getPersonalidade().getNome(), simulacao.getZumbis().size(),
                simulacao.getZumbisAbatidos()), (float) ESQUERDA_DO_MAPA, yTexto);

        // Canto superior direito: vida e itens
        String itens = "Itens: " + sobrevivente.getItensNaBolsa();
        int larguraItens = g.getFontMetrics().stringWidth(itens);
        double xItens = DIREITA_DO_MAPA - larguraItens;
        g.drawString(itens, (float) xItens, yTexto);

        double xBarra = xItens - 20 - 160;
        desenharBarra(g, xBarra, yTexto - 12, 160, 14,
                sobrevivente.getVida() / sobrevivente.getVidaMaxima(), new Color(60, 190, 80));
        g.setColor(Color.WHITE);
        g.drawString("Vida", (float) (xBarra - 40), yTexto);
        g.setColor(Color.BLACK);
        g.setFont(new Font("SansSerif", Font.BOLD, 11));
        g.drawString(String.format("%.0f", sobrevivente.getVida()), (float) (xBarra + 70), yTexto - 1);

        // Embaixo: como controlar
        g.setColor(new Color(190, 190, 190));
        g.setFont(new Font("SansSerif", Font.PLAIN, 12));
        String dica = opcoes.modo == Opcoes.ModoDeJogo.TESTE && !opcoes.modoAutomatico
                ? "Setas/WASD movem o zumbi de borda branca  ·  Clique no mapa para mandá-lo até um ponto  ·  Opções na janela Debug"
                : "Zumbis no automático  ·  Opções na janela Debug";
        g.drawString(dica, (float) ESQUERDA_DO_MAPA, (float) (BASE_DO_MAPA + 45));
    }

    private void desenharFimDeJogo(Graphics2D g, Sobrevivente sobrevivente, int largura, int altura) {
        g.setColor(new Color(0, 0, 0, 190));
        g.fill(new Rectangle2D.Double(-largura / 2.0, -altura / 2.0, largura, altura));

        g.setColor(new Color(220, 50, 50));
        g.setFont(new Font("SansSerif", Font.BOLD, 48));
        desenharTextoCentralizado(g, "FIM DE JOGO", -30);

        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.PLAIN, 20));
        desenharTextoCentralizado(g, sobrevivente.getPersonalidade().getNome() + " foi pego pelos zumbis.", 15);
        desenharTextoCentralizado(g, String.format("Itens coletados: %d   |   Zumbis abatidos: %d",
                sobrevivente.getItensNaBolsa(), simulacao.getZumbisAbatidos()), 50);
    }

    // =====================================================================
    //  AJUDANTES DE DESENHO
    // =====================================================================

    private static void desenharBarra(Graphics2D g, double x, double y, double largura, double altura,
                                      double fracao, Color cor) {
        g.setColor(new Color(60, 60, 60));
        g.fill(new Rectangle2D.Double(x, y, largura, altura));
        g.setColor(cor);
        g.fill(new Rectangle2D.Double(x, y, largura * Math.clamp(fracao, 0.0, 1.0), altura));
        g.setColor(Color.BLACK);
        g.setStroke(new BasicStroke(1f));
        g.draw(new Rectangle2D.Double(x, y, largura, altura));
    }

    private static void desenharCirculo(Graphics2D g, Vector2 centro, double raio, Color cor, boolean tracejado) {
        g.setColor(cor);
        g.setStroke(tracejado
                ? new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[]{6f, 6f}, 0f)
                : new BasicStroke(1.5f));
        g.draw(new Ellipse2D.Double(centro.x - raio, centro.y - raio, 2 * raio, 2 * raio));
    }

    private static void desenharSeta(Graphics2D g, Vector2 origem, Vector2 vetor, Color cor, float espessura) {
        if (vetor.size() < 2) return;
        Vector2 ponta = add(origem, vetor);
        double angulo = Math.atan2(vetor.y, vetor.x);
        Vector2 aba1 = subtract(ponta, byAngleSize(angulo - Math.toRadians(25), 7));
        Vector2 aba2 = subtract(ponta, byAngleSize(angulo + Math.toRadians(25), 7));

        g.setColor(cor);
        g.setStroke(new BasicStroke(espessura, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new Line2D.Double(origem.x, origem.y, ponta.x, ponta.y));
        g.draw(new Line2D.Double(ponta.x, ponta.y, aba1.x, aba1.y));
        g.draw(new Line2D.Double(ponta.x, ponta.y, aba2.x, aba2.y));
    }

    private static void desenharTextoCentralizado(Graphics2D g, String texto, double y) {
        int larguraDoTexto = g.getFontMetrics().stringWidth(texto);
        g.drawString(texto, (float) (-larguraDoTexto / 2.0), (float) y);
    }
}

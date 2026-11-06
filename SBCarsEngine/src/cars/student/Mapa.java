package cars.student;

import cars.engine.Vector2;

import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

import static cars.engine.Vector2.*;

/**
 * Planta do escritório. Cada caractere é uma célula de 30x30 pixels:
 *   '#' = parede
 *   'M' = móvel (mesa, estante)
 *   '.' = chão
 * Paredes e móveis são obstáculos: bloqueiam movimento, visão e tiros.
 * A diferença entre eles é só no desenho.
 *
 * Também guarda a ROTA DE PATRULHA dos zumbis (usada pelo Path Following).
 */
public class Mapa {

    public static final int TAMANHO_DA_CELULA = 30;

    private static final String[] PLANTA = {
            "################################",
            "#........#.........#...........#",
            "#........#.........#...........#",
            "#..MMMM..#..MMMMM..#..MM..MM...#",
            "#..................#...........#",
            "#..................#...........#",
            "####..#######..########..###..##",
            "#..............................#",
            "#..............................#",
            "#..............................#",
            "####..########..#########..#####",
            "#.........#.........#..........#",
            "#.........#.........#..........#",
            "#..MMMM...#..MMMM...#..M...M...#",
            "#..MMMM...#..MMMM...#..M...M...#",
            "#...................#..M...M...#",
            "#...................#..M...M...#",
            "#.........#.........#..........#",
            "#.........#.........#..........#",
            "################################",
    };

    /**
     * Rota de patrulha: pontos em (coluna, linha) da planta. ".5" = entre duas células,
     * por exemplo no meio de uma porta de 2 células. A rota passa pelo corredor central
     * e entra em todas as salas; do último ponto ela volta para o primeiro.
     */
    private static final double[][] ROTA_DE_PATRULHA = {
            {4.5, 8}, {4.5, 4.5}, {13.5, 4.5}, {13.5, 8},          // sala de cima à esquerda
            {23.5, 8}, {23.5, 4.5}, {28.5, 4.5}, {28.5, 8},        // sala de cima à direita
            {25.5, 8}, {25.5, 17.5}, {29, 17.5}, {29, 11.5},       // sala de baixo à direita (volta)
            {25.5, 11.5}, {25.5, 8},
            {14.5, 8}, {14.5, 11.5}, {18, 11.5}, {18, 15.5},       // salas de baixo (meio e esquerda)
            {8, 15.5}, {8, 11.5}, {4.5, 11.5},
    };
    /** Meia largura do "tubo" da rota: dentro dele o zumbi já está no caminho. */
    private static final double RAIO_DA_ROTA = 15;

    public static final int LARGURA = PLANTA[0].length() * TAMANHO_DA_CELULA;
    public static final int ALTURA = PLANTA.length * TAMANHO_DA_CELULA;

    private final List<Rectangle2D> paredes = new ArrayList<>();
    private final List<Rectangle2D> moveis = new ArrayList<>();
    private final List<Rectangle2D> obstaculos = new ArrayList<>(); // paredes + móveis
    private final List<Vector2> pontosAbertos = new ArrayList<>();
    private final Caminho rotaDePatrulha;

    /**
     * Resultado de um raio que bateu num obstáculo: a que distância, em que ponto
     * e a NORMAL da face atingida (vetor unitário apontando para fora da parede).
     */
    public record Colisao(double distancia, Vector2 ponto, Vector2 normal) { }

    public Mapa() {
        for (int linha = 0; linha < PLANTA.length; linha++) {
            for (int coluna = 0; coluna < PLANTA[linha].length(); coluna++) {
                char celula = PLANTA[linha].charAt(coluna);
                if (celula == '#') paredes.add(retanguloDaCelula(linha, coluna));
                if (celula == 'M') moveis.add(retanguloDaCelula(linha, coluna));
                if (ehPontoAberto(linha, coluna)) pontosAbertos.add(centroDaCelula(linha, coluna));
            }
        }
        obstaculos.addAll(paredes);
        obstaculos.addAll(moveis);

        List<Vector2> pontosDaRota = new ArrayList<>();
        for (double[] ponto : ROTA_DE_PATRULHA) pontosDaRota.add(pontoDaPlanta(ponto[0], ponto[1]));
        rotaDePatrulha = new Caminho(pontosDaRota, RAIO_DA_ROTA);
    }

    // ---------------------------------------------------------------------
    //  Leitura da planta
    // ---------------------------------------------------------------------

    /**
     * Chão cercado de chão. Esses pontos servem para nascer (nada nasce encostado em parede)
     * e como destinos de fuga e de exploração do sobrevivente.
     */
    private boolean ehPontoAberto(int linha, int coluna) {
        return ehChao(linha, coluna)
                && ehChao(linha - 1, coluna) && ehChao(linha + 1, coluna)
                && ehChao(linha, coluna - 1) && ehChao(linha, coluna + 1);
    }

    private boolean ehChao(int linha, int coluna) {
        boolean dentroDaPlanta = linha >= 0 && linha < PLANTA.length && coluna >= 0 && coluna < PLANTA[linha].length();
        return dentroDaPlanta && PLANTA[linha].charAt(coluna) == '.';
    }

    /** Converte linha/coluna da planta para coordenadas do mundo (origem no centro da tela). */
    private Rectangle2D retanguloDaCelula(int linha, int coluna) {
        double x = -LARGURA / 2.0 + coluna * TAMANHO_DA_CELULA;
        double y = -ALTURA / 2.0 + linha * TAMANHO_DA_CELULA;
        return new Rectangle2D.Double(x, y, TAMANHO_DA_CELULA, TAMANHO_DA_CELULA);
    }

    private Vector2 centroDaCelula(int linha, int coluna) {
        return pontoDaPlanta(coluna, linha);
    }

    /** Centro de uma posição da planta; aceita meia célula (ex.: coluna 4.5 = entre a 4 e a 5). */
    private static Vector2 pontoDaPlanta(double coluna, double linha) {
        double x = -LARGURA / 2.0 + (coluna + 0.5) * TAMANHO_DA_CELULA;
        double y = -ALTURA / 2.0 + (linha + 0.5) * TAMANHO_DA_CELULA;
        return vec2(x, y);
    }

    // ---------------------------------------------------------------------
    //  Consultas usadas pelos agentes e pela simulação
    // ---------------------------------------------------------------------

    /** Linha de visão livre se nenhum obstáculo corta o segmento entre os dois pontos. */
    public boolean temLinhaDeVisao(Vector2 de, Vector2 ate) {
        for (Rectangle2D obstaculo : obstaculos) {
            if (obstaculo.intersectsLine(de.x, de.y, ate.x, ate.y)) return false;
        }
        return true;
    }

    public boolean ehObstaculo(Vector2 ponto) {
        for (Rectangle2D obstaculo : obstaculos) {
            if (obstaculo.contains(ponto.x, ponto.y)) return true;
        }
        return false;
    }

    /** Colisão: se o círculo do agente entrou num obstáculo, empurra ele de volta para fora. */
    public Vector2 empurrarParaFora(Vector2 posicao, double raio) {
        Vector2 corrigida = posicao.clone();
        for (Rectangle2D obstaculo : obstaculos) {
            Vector2 paraFora = subtract(corrigida, pontoMaisProximo(obstaculo, corrigida));
            double distancia = paraFora.size();
            if (distancia > 0 && distancia < raio) {
                corrigida.add(multiply(normalize(paraFora), raio - distancia));
            }
        }
        return corrigida;
    }

    private static Vector2 pontoMaisProximo(Rectangle2D retangulo, Vector2 ponto) {
        double x = Math.clamp(ponto.x, retangulo.getMinX(), retangulo.getMaxX());
        double y = Math.clamp(ponto.y, retangulo.getMinY(), retangulo.getMaxY());
        return vec2(x, y);
    }

    /**
     * Passagem livre para um corpo de raio "raio": além da linha central, as duas linhas
     * paralelas nas bordas do corpo também não podem cortar nenhum obstáculo.
     * (Linha de visão é só a linha central: o olho passa rente a uma quina, o corpo não.)
     */
    public boolean temPassagem(Vector2 de, Vector2 ate, double raio) {
        Vector2 lado = multiply(perp(normalize(subtract(ate, de))), raio);
        return temLinhaDeVisao(de, ate)
                && temLinhaDeVisao(add(de, lado), add(ate, lado))
                && temLinhaDeVisao(subtract(de, lado), subtract(ate, lado));
    }

    /**
     * Pontos abertos entre as distâncias mínima e máxima para onde o agente consegue
     * ir em linha reta (o corpo inteiro passa). São os candidatos a destino de
     * exploração e a ponto de fuga.
     */
    public List<Vector2> pontosAlcancaveis(Vector2 de, double distanciaMinima, double distanciaMaxima, double raioDoCorpo) {
        List<Vector2> alcancaveis = new ArrayList<>();
        for (Vector2 ponto : pontosAbertos) {
            double distancia = distance(de, ponto);
            if (distancia >= distanciaMinima && distancia <= distanciaMaxima && temPassagem(de, ponto, raioDoCorpo)) {
                alcancaveis.add(ponto.clone());
            }
        }
        return alcancaveis;
    }

    /**
     * Lança um raio (origem + direção) e devolve o PRIMEIRO obstáculo que ele atinge
     * até o alcance, ou null se o caminho está livre. Usado pelas antenas do
     * Obstacle Avoidance e pela área visível (lanterna).
     */
    public Colisao lancarRaio(Vector2 origem, Vector2 direcao, double alcance) {
        Vector2 dir = normalize(direcao);
        Colisao maisPerto = null;
        for (Rectangle2D obstaculo : obstaculos) {
            Colisao colisao = raioContraRetangulo(origem, dir, obstaculo);
            if (colisao != null && colisao.distancia() <= alcance
                    && (maisPerto == null || colisao.distancia() < maisPerto.distancia())) {
                maisPerto = colisao;
            }
        }
        return maisPerto;
    }

    /**
     * Raio contra um retângulo (método das "fatias"). Para cada eixo calculamos em que trecho
     * do raio ele está entre os dois lados do retângulo; o raio só bate se os trechos de X e Y
     * se cruzam. A face atingida é a do eixo em que ele entrou por último, e a normal aponta
     * para fora dessa face, contra o raio.
     */
    private static Colisao raioContraRetangulo(Vector2 origem, Vector2 dir, Rectangle2D r) {
        double entrada = 0;
        double saida = Double.POSITIVE_INFINITY;
        Vector2 normal = null;

        // Eixo X: entre a esquerda e a direita do retângulo
        if (dir.x != 0) {
            double t1 = (r.getMinX() - origem.x) / dir.x;
            double t2 = (r.getMaxX() - origem.x) / dir.x;
            if (Math.min(t1, t2) > entrada) {
                entrada = Math.min(t1, t2);
                normal = vec2(dir.x > 0 ? -1 : 1, 0); // bateu na face esquerda ou direita
            }
            saida = Math.min(saida, Math.max(t1, t2));
        } else if (origem.x < r.getMinX() || origem.x > r.getMaxX()) {
            return null;
        }

        // Eixo Y: entre o topo e a base
        if (dir.y != 0) {
            double t1 = (r.getMinY() - origem.y) / dir.y;
            double t2 = (r.getMaxY() - origem.y) / dir.y;
            if (Math.min(t1, t2) > entrada) {
                entrada = Math.min(t1, t2);
                normal = vec2(0, dir.y > 0 ? -1 : 1); // bateu na face de cima ou de baixo
            }
            saida = Math.min(saida, Math.max(t1, t2));
        } else if (origem.y < r.getMinY() || origem.y > r.getMaxY()) {
            return null;
        }

        if (entrada > saida) return null;           // os trechos não se cruzam: errou o retângulo
        if (normal == null) normal = negate(dir);    // origem já dentro (não acontece com a colisão ligada)
        return new Colisao(entrada, add(origem, multiply(dir, entrada)), normal);
    }

    /**
     * Área que realmente dá para ver de um ponto (para a lanterna e o debug).
     * Lança vários raios em círculo; cada raio para no primeiro obstáculo que encontra.
     */
    public Path2D areaVisivel(Vector2 origem, double raio) {
        final int quantidadeDeRaios = 240;
        Path2D area = new Path2D.Double();
        for (int i = 0; i < quantidadeDeRaios; i++) {
            Vector2 direcao = byAngle(2 * Math.PI * i / quantidadeDeRaios);
            Colisao colisao = lancarRaio(origem, direcao, raio);
            double alcance = colisao == null ? raio : colisao.distancia();

            Vector2 ponta = add(origem, multiply(direcao, alcance));
            if (i == 0) area.moveTo(ponta.x, ponta.y); else area.lineTo(ponta.x, ponta.y);
        }
        area.closePath();
        return area;
    }

    /** Centro de uma célula livre qualquer (nunca em cima de parede ou móvel). */
    public Vector2 posicaoLivreAleatoria() {
        int sorteado = (int) (Math.random() * pontosAbertos.size());
        return pontosAbertos.get(sorteado).clone();
    }

    public Caminho getRotaDePatrulha() { return rotaDePatrulha; }
    public List<Rectangle2D> getParedes() { return paredes; }
    public List<Rectangle2D> getMoveis() { return moveis; }
}

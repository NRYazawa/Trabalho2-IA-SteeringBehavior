package cars.student;

import cars.engine.Vector2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static cars.engine.Vector2.*;

/**
 * Um caminho para o PATH FOLLOWING: uma sequência de pontos ligados por segmentos
 * retos, formando um circuito fechado (do último ponto volta para o primeiro).
 *
 * O caminho tem um RAIO: imagine um "tubo" em volta da linha. Dentro do tubo o agente
 * está no caminho; fora dele, precisa voltar.
 *
 * Importante: isto NÃO é pathfinding. Ninguém calcula rota nenhuma; o caminho é fixo
 * (desenhado à mão na planta do escritório) e o agente só o segue.
 */
public class Caminho {

    /**
     * Onde um ponto "cai" no caminho: o segmento, o ponto mais próximo dentro dele
     * e a distância até lá.
     */
    public record Projecao(int segmento, Vector2 ponto, double distancia) { }

    private final List<Vector2> pontos;
    private final double raio;

    public Caminho(List<Vector2> pontos, double raio) {
        if (pontos.size() < 2) throw new IllegalArgumentException("o caminho precisa de pelo menos 2 pontos");
        this.pontos = List.copyOf(pontos);
        this.raio = raio;
    }

    /** O mesmo caminho percorrido no sentido contrário. */
    public Caminho invertido() {
        List<Vector2> contrario = new ArrayList<>(pontos);
        Collections.reverse(contrario);
        return new Caminho(contrario, raio);
    }

    // ---------------------------------------------------------------------
    //  Segmentos (o segmento i vai do ponto i até o ponto i+1; o último fecha o circuito)
    // ---------------------------------------------------------------------

    public int quantidadeDeSegmentos() {
        return pontos.size();
    }

    public int proximoSegmento(int segmento) {
        return (segmento + 1) % pontos.size();
    }

    public Vector2 inicioDoSegmento(int segmento) {
        return pontos.get(segmento).clone();
    }

    public Vector2 fimDoSegmento(int segmento) {
        return pontos.get(proximoSegmento(segmento)).clone();
    }

    /** Vetor unitário apontando para onde o segmento "anda". */
    public Vector2 direcaoDoSegmento(int segmento) {
        return normalize(subtract(fimDoSegmento(segmento), inicioDoSegmento(segmento)));
    }

    // ---------------------------------------------------------------------
    //  Consultas usadas pelo Path Following
    // ---------------------------------------------------------------------

    /**
     * Projeção (ponto normal) de um ponto sobre UM segmento: o ponto do segmento
     * mais próximo dele. É o "pé da perpendicular", preso entre o início e o fim.
     */
    public Projecao projetar(Vector2 ponto, int segmento) {
        Vector2 inicio = inicioDoSegmento(segmento);
        Vector2 fim = fimDoSegmento(segmento);
        Vector2 segmentoInteiro = subtract(fim, inicio);

        // t = quanto do segmento é preciso andar para chegar no pé da perpendicular (0 = início, 1 = fim)
        double t = subtract(ponto, inicio).dot(segmentoInteiro) / segmentoInteiro.sizeSqr();
        t = Math.clamp(t, 0.0, 1.0);

        Vector2 pe = add(inicio, multiply(segmentoInteiro, t));
        return new Projecao(segmento, pe, distance(ponto, pe));
    }

    /**
     * Anda uma certa distância pelo caminho a partir de uma projeção, virando para o
     * próximo segmento se este acabar. Serve para achar o alvo "um pouco à frente".
     */
    public Vector2 avancar(Projecao projecao, double distancia) {
        int segmento = projecao.segmento();
        Vector2 posicao = projecao.ponto();
        double falta = distancia;

        while (true) {
            Vector2 fim = fimDoSegmento(segmento);
            double ateOFim = Vector2.distance(posicao, fim);
            if (falta <= ateOFim) {
                return add(posicao, multiply(direcaoDoSegmento(segmento), falta));
            }
            falta -= ateOFim;
            posicao = fim;
            segmento = proximoSegmento(segmento);
        }
    }

    /**
     * Para ENTRAR no caminho: a projeção mais próxima que o agente consegue ver
     * (sem parede no meio) e que está a até "alcance" de distância. Null se não há nenhuma.
     */
    public Projecao projecaoVisivelMaisProxima(Vector2 posicao, Mapa mapa, double alcance) {
        Projecao melhor = null;
        for (int segmento = 0; segmento < quantidadeDeSegmentos(); segmento++) {
            Projecao projecao = projetar(posicao, segmento);
            boolean perto = projecao.distancia() <= alcance;
            boolean melhorQueAAtual = melhor == null || projecao.distancia() < melhor.distancia();
            if (perto && melhorQueAAtual && mapa.temLinhaDeVisao(posicao, projecao.ponto())) {
                melhor = projecao;
            }
        }
        return melhor;
    }

    public List<Vector2> getPontos() {
        List<Vector2> copia = new ArrayList<>();
        for (Vector2 ponto : pontos) copia.add(ponto.clone());
        return copia;
    }

    public double getRaio() {
        return raio;
    }
}

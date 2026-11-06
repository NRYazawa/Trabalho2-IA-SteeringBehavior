package cars.student.agentes;

import cars.engine.Car;
import cars.engine.Vector2;
import cars.engine.World;
import cars.student.Caminho;
import cars.student.Mapa;
import cars.student.Simulacao;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static cars.engine.Vector2.*;

/**
 * Base do Sobrevivente e do Zumbi: tudo que os dois têm em comum.
 *
 * - os comportamentos de steering:
 *     simples:   Seek, Flee, Arrive, Wander;
 *     avançados: Obstacle Avoidance, Path Following e Separation;
 * - a combinação das forças com pesos e o limite de força máxima;
 * - vida, percepção (visão) e colisão com as paredes.
 *
 * Quem decide QUAIS comportamentos usar em cada momento são os estados.
 */
public abstract class Agente extends Car {

    /** Metade do lado do quadradinho. Usado no desenho e na colisão. */
    public static final double RAIO = 10;

    // Obstacle Avoidance: três "antenas" (bigodes) à frente do agente.
    private static final double PESO_OBSTACLE_AVOIDANCE = 2.0;
    private static final double ANTENA_COM_O_AGENTE_PARADO = 25;      // px
    private static final double ANTENA_SEGUNDOS_A_FRENTE = 0.5;       // + o que ele anda em 0,5 s
    private static final double ANGULO_DAS_ANTENAS_LATERAIS = Math.toRadians(35);
    private static final double TAMANHO_DAS_ANTENAS_LATERAIS = 0.7;   // 70% da antena da frente

    // Path Following: olha onde vai estar daqui a pouco e mira um ponto adiante na rota.
    private static final double CAMINHO_SEGUNDOS_DE_PREVISAO = 0.5;
    private static final double CAMINHO_DISTANCIA_DO_ALVO = 40;       // px adiante da projeção

    // Wander: um círculo projetado à frente do agente; o alvo passeia pela borda dele.
    private static final double WANDER_DISTANCIA_DO_CIRCULO = 60;
    private static final double WANDER_RAIO_DO_CIRCULO = 30;
    private static final double WANDER_VARIACAO_POR_SEGUNDO = 5; // em radianos

    protected final Simulacao simulacao;
    private final Color cor;
    private final double vidaMaxima;
    private double vida;
    private double anguloWander = 0;

    /** Uma antena do Obstacle Avoidance: de onde sai, até onde vai e se bateu (null = livre). */
    public record Antena(Vector2 inicio, Vector2 fim, Mapa.Colisao colisao) { }
    private List<Antena> antenas = List.of();

    /**
     * Resultado do Path Following, guardado para o debug desenhar:
     * posição futura prevista, onde ela cai na rota, o alvo adiante e se estava dentro do tubo.
     */
    public record Seguimento(Vector2 forca, int segmento, Vector2 futuro, Vector2 projecao,
                             Vector2 alvo, boolean dentroDoCaminho) { }

    /** Forças deste frame, separadas por comportamento (para desenhar no debug). */
    private final Map<Comportamento, Vector2> forcas = new EnumMap<>(Comportamento.class);
    private final Map<Comportamento, Double> pesos = new EnumMap<>(Comportamento.class);
    private Vector2 resultante = vec2();

    /** Uma linha do painel "ao vivo": qual força, com que peso e com que intensidade (tamanho da seta). */
    public record ForcaAplicada(Comportamento comportamento, double peso, double intensidade) { }

    /**
     * Cópia das forças do último frame para a janela de Debug.
     * É "volatile" e imutável porque a janela de Debug lê de outra thread.
     */
    private volatile List<ForcaAplicada> ultimasForcas = List.of();
    private volatile double intensidadeDaUltimaResultante = 0;

    protected Agente(Simulacao simulacao, Vector2 posicao, Color cor,
                     double velocidadeMaxima, double forcaMaxima, double vidaMaxima) {
        super(settings -> settings
                .position(posicao)
                .color(cor)
                .maxSpeed(velocidadeMaxima)
                .maxForce(forcaMaxima)
                .randomOrientation());
        this.simulacao = simulacao;
        this.cor = cor;
        this.vidaMaxima = vidaMaxima;
        this.vida = vidaMaxima;
    }

    // =====================================================================
    //  COMO O MOVIMENTO ACONTECE (chamado pela engine a cada frame)
    // =====================================================================

    @Override
    public final Vector2 calculateSteering(World world) {
        forcas.clear();
        pesos.clear();

        // 1) Percepção + geração de forças: o estado atual escolhe os comportamentos.
        //    O Obstacle Avoidance vale em todos os estados, então é somado aqui.
        executarEstado(world);
        aplicarForca(Comportamento.OBSTACLE_AVOIDANCE, obstacleAvoidance(), PESO_OBSTACLE_AVOIDANCE);

        // 2) Combinação: soma de todas as forças (já multiplicadas pelos pesos)
        Vector2 soma = vec2();
        for (Vector2 forca : forcas.values()) {
            soma.add(forca);
        }

        // 3) Limitação: a resultante nunca passa da força máxima (evita "trancos")
        resultante = truncate(soma, getMaxForce());
        guardarResumoParaODebug();

        // 4) A engine usa a resultante para mudar a velocidade, e a velocidade muda a posição
        return resultante;
    }

    /** Cada tipo de agente executa o seu estado atual aqui. */
    protected abstract void executarEstado(World world);

    /** Soma uma força (já multiplicada pelo peso) na resultante deste frame. */
    public void aplicarForca(Comportamento comportamento, Vector2 forca, double peso) {
        Vector2 forcaComPeso = multiply(forca, peso);
        forcas.merge(comportamento, forcaComPeso, (atual, nova) -> add(atual, nova));
        pesos.merge(comportamento, peso, Double::sum); // ex.: Flee de 2 zumbis com peso 0.5 = peso 1.0
    }

    private void guardarResumoParaODebug() {
        List<ForcaAplicada> resumo = new ArrayList<>();
        for (Comportamento comportamento : forcas.keySet()) {
            resumo.add(new ForcaAplicada(comportamento, pesos.get(comportamento), forcas.get(comportamento).size()));
        }
        ultimasForcas = List.copyOf(resumo);
        intensidadeDaUltimaResultante = resultante.size();
    }

    // =====================================================================
    //  COMPORTAMENTOS DE STEERING
    //  Seek, Flee e Arrive seguem a mesma ideia: força = velocidade desejada - velocidade atual.
    //  Wander, Separation e Obstacle Avoidance devolvem direto o "empurrão" que querem dar.
    //  Path Following reaproveita o Seek (mira um ponto da rota).
    // =====================================================================

    /** SEEK: "quero ir até lá na velocidade máxima". */
    public Vector2 seek(Vector2 alvo) {
        return seek(alvo, getMaxSpeed());
    }

    /** SEEK com uma velocidade desejada menor que a máxima (usado na ronda dos zumbis). */
    public Vector2 seek(Vector2 alvo, double velocidade) {
        Vector2 desejada = multiply(normalize(subtract(alvo, getPosition())), velocidade);
        return subtract(desejada, getVelocity());
    }

    /** FLEE: o contrário do seek, a velocidade desejada aponta para longe da ameaça. */
    public Vector2 flee(Vector2 ameaca) {
        Vector2 desejada = multiply(normalize(subtract(getPosition(), ameaca)), getMaxSpeed());
        return subtract(desejada, getVelocity());
    }

    /** ARRIVE: igual ao seek, mas desacelera dentro do raio de frenagem para parar no alvo. */
    public Vector2 arrive(Vector2 alvo, double raioDeFrenagem) {
        Vector2 paraOAlvo = subtract(alvo, getPosition());
        double distancia = paraOAlvo.size();
        double velocidade = getMaxSpeed() * Math.min(1.0, distancia / raioDeFrenagem);
        Vector2 desejada = multiply(normalize(paraOAlvo), velocidade);
        return subtract(desejada, getVelocity());
    }

    /**
     * WANDER: projeta um círculo à frente e mira num ponto da borda dele.
     * A cada frame esse ponto anda um pouquinho, o que dá um zigue-zague suave
     * em vez de um "aleatório maluco".
     */
    public Vector2 wander(double secs) {
        anguloWander += (Math.random() * 2 - 1) * WANDER_VARIACAO_POR_SEGUNDO * secs;

        Vector2 frente = getDirection();
        double anguloDaFrente = Math.atan2(frente.y, frente.x);

        Vector2 centroDoCirculo = multiply(frente, WANDER_DISTANCIA_DO_CIRCULO);
        Vector2 pontoNaBorda = byAngleSize(anguloDaFrente + anguloWander, WANDER_RAIO_DO_CIRCULO);
        return add(centroDoCirculo, pontoNaBorda);
    }

    /**
     * SEPARATION: cada vizinho dentro do raio gera um empurrão para longe dele.
     * Quanto mais perto, mais forte (1 = colado, 0 = na borda do raio).
     */
    public Vector2 separation(List<? extends Car> vizinhos, double raio) {
        Vector2 soma = vec2();
        for (Car vizinho : vizinhos) {
            Vector2 paraLonge = subtract(getPosition(), vizinho.getPosition());
            double distancia = paraLonge.size();
            if (distancia > 0 && distancia < raio) {
                double intensidade = 1 - distancia / raio;
                soma.add(multiply(normalize(paraLonge), intensidade * getMaxForce()));
            }
        }
        return soma;
    }

    /**
     * OBSTACLE AVOIDANCE com antenas (os "bigodes" do Reynolds / "feelers" do Buckland).
     *
     * Três raios saem do centro do agente: um reto para frente e dois abertos 35° para os lados.
     * A antena da frente cresce com a velocidade: quanto mais rápido, mais cedo ele precisa
     * enxergar a parede. Se uma antena atravessa um obstáculo, a "invasão" é o pedaço da antena
     * que ficou dentro da parede, de 0 (só encostou na ponta) a 1 (a parede está colada nele).
     *
     * Força = normal da face atingida × invasão × força máxima.
     * Só a antena com a maior invasão conta: ela é a ameaça mais urgente.
     *
     * Exemplos: indo reto contra uma parede, a normal aponta para trás e ele freia/desvia;
     * andando rente a uma parede, a antena lateral bate e a normal empurra para o meio do corredor.
     */
    public Vector2 obstacleAvoidance() {
        Mapa mapa = simulacao.getMapa();
        Vector2 posicao = getPosition();
        Vector2 frente = getDirection();
        double comprimento = ANTENA_COM_O_AGENTE_PARADO + getSpeed() * ANTENA_SEGUNDOS_A_FRENTE;
        double comprimentoLateral = comprimento * TAMANHO_DAS_ANTENAS_LATERAIS;

        List<Antena> novasAntenas = List.of(
                criarAntena(mapa, posicao, frente, comprimento),
                criarAntena(mapa, posicao, rotate(frente, ANGULO_DAS_ANTENAS_LATERAIS), comprimentoLateral),
                criarAntena(mapa, posicao, rotate(frente, -ANGULO_DAS_ANTENAS_LATERAIS), comprimentoLateral));
        antenas = novasAntenas;

        Vector2 forca = vec2();
        double maiorInvasao = 0;
        for (Antena antena : novasAntenas) {
            if (antena.colisao() == null) continue;
            double tamanho = Vector2.distance(antena.inicio(), antena.fim());
            double invasao = (tamanho - antena.colisao().distancia()) / tamanho;
            if (invasao > maiorInvasao) {
                maiorInvasao = invasao;
                forca = multiply(antena.colisao().normal(), invasao * getMaxForce());
            }
        }
        return forca;
    }

    private static Antena criarAntena(Mapa mapa, Vector2 posicao, Vector2 direcao, double comprimento) {
        Vector2 fim = add(posicao, multiply(direcao, comprimento));
        return new Antena(posicao, fim, mapa.lancarRaio(posicao, direcao, comprimento));
    }

    /**
     * PATH FOLLOWING (Reynolds), seguindo um caminho com raio (um "tubo").
     *
     * 1) Previsão: onde estarei daqui a 0,5 s se continuar assim? (posição + velocidade × 0,5)
     * 2) Projeção: o ponto da rota mais próximo dessa posição futura. Só olhamos o segmento
     *    atual e o próximo; se o próximo ficou tão perto quanto (ou mais), avançamos — é assim
     *    que ele faz a curva, e o empate acontece exatamente na quina.
     * 3) Alvo: andamos 40 px pela rota a partir da projeção (o "ponto adiante").
     * 4) Força (as duas na "velocidade" pedida, que pode ser menor que a máxima):
     *    - fora do tubo (a posição futura vai sair do caminho): Seek no alvo, para voltar;
     *    - dentro do tubo: só mantém o passo na direção do segmento
     *      (desejada = direção do segmento × velocidade; força = desejada − velocidade atual).
     *      Não puxa para o centro da linha, então vários zumbis andam lado a lado.
     */
    public Seguimento pathFollowing(Caminho caminho, int segmentoAtual, double velocidade) {
        Vector2 futuro = add(getPosition(), multiply(getVelocity(), CAMINHO_SEGUNDOS_DE_PREVISAO));

        Caminho.Projecao projecao = caminho.projetar(futuro, segmentoAtual);
        Caminho.Projecao noProximo = caminho.projetar(futuro, caminho.proximoSegmento(segmentoAtual));
        if (noProximo.distancia() <= projecao.distancia()) projecao = noProximo; // empate (na quina) = avança

        Vector2 alvo = caminho.avancar(projecao, CAMINHO_DISTANCIA_DO_ALVO);
        boolean dentro = projecao.distancia() <= caminho.getRaio();

        Vector2 forca;
        if (dentro) {
            Vector2 desejada = multiply(caminho.direcaoDoSegmento(projecao.segmento()), velocidade);
            forca = subtract(desejada, getVelocity());
        } else {
            forca = seek(alvo, velocidade);
        }
        return new Seguimento(forca, projecao.segmento(), futuro, projecao.ponto(), alvo, dentro);
    }

    // =====================================================================
    //  PERCEPÇÃO, COLISÃO E VIDA
    // =====================================================================

    /** Vê um ponto se ele está dentro do raio E nenhuma parede bloqueia a linha até ele. */
    public boolean consegueVer(Vector2 ponto, double raio) {
        // "Vector2.distance" por extenso: a classe Car da engine também tem um distance(Car, Car)
        return Vector2.distance(getPosition(), ponto) <= raio
                && simulacao.getMapa().temLinhaDeVisao(getPosition(), ponto);
    }

    /** Nenhum obstáculo entre o agente e o ponto (sem limite de distância). */
    public boolean temLinhaDeVisaoAte(Vector2 ponto) {
        return simulacao.getMapa().temLinhaDeVisao(getPosition(), ponto);
    }

    /** Depois de se mover, a engine chama este gancho: se entrou numa parede, é empurrado para fora. */
    @Override
    protected Vector2 constrainPosition(Vector2 novaPosicao) {
        return simulacao.getMapa().empurrarParaFora(novaPosicao, RAIO);
    }

    public void receberDano(double dano) {
        vida = Math.max(0, vida - dano);
    }

    public boolean estaMorto() {
        return vida <= 0;
    }

    public double getVida() { return vida; }
    public double getVidaMaxima() { return vidaMaxima; }
    public Color getCor() { return cor; }
    public Simulacao getSimulacao() { return simulacao; }
    public Vector2 getResultante() { return resultante.clone(); }
    public Map<Comportamento, Vector2> getForcas() { return Collections.unmodifiableMap(forcas); }
    public List<ForcaAplicada> getUltimasForcas() { return ultimasForcas; }
    public double getIntensidadeDaUltimaResultante() { return intensidadeDaUltimaResultante; }
    public List<Antena> getAntenas() { return antenas; }

    /** Para onde o rosto aponta no desenho. Por padrão, para onde está andando. */
    public Vector2 getDirecaoDoOlhar() {
        return getDirection();
    }

    /** Nome do estado atual (para o debug). */
    public abstract String getNomeDoEstado();
}

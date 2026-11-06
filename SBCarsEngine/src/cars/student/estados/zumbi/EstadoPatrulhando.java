package cars.student.estados.zumbi;

import cars.engine.World;
import cars.student.Caminho;
import cars.student.agentes.Agente.Seguimento;
import cars.student.agentes.Comportamento;
import cars.student.agentes.Zumbi;
import cars.student.fsm.AbstractState;

/**
 * Zumbi automático fazendo a ronda pelo escritório: PATH FOLLOWING na rota de patrulha
 * + SEPARATION (para não andarem em fila colados uns nos outros).
 *
 * Ao entrar, ele procura o ponto da rota mais próximo que consegue ver e começa dali.
 * Se não enxerga a rota (está no fundo de uma sala), volta a vagar até achá-la.
 *
 * Dois zumbis em sentidos opostos podem se encontrar de frente numa porta: o Path Following
 * empurra cada um para a frente e o Separation empurra para trás, e as forças se anulam.
 * Se ficar parado assim por um tempo, ele "se perde" e vai vagar um pouco (Wander desempata).
 */
public class EstadoPatrulhando extends AbstractState<Zumbi> {

    private static final double PESO_PATH_FOLLOWING = 1.0;
    private static final double PESO_SEPARATION = 1.5;
    /** Na ronda ele anda arrastado; só corre (velocidade máxima) quando persegue. */
    private static final double FRACAO_DA_VELOCIDADE_NA_RONDA = 0.5;
    private static final double VELOCIDADE_DE_QUEM_ESTA_PARADO = 5;
    private static final double SEGUNDOS_PARADO_PARA_DESISTIR = 1.5;

    private int segmento = -1; // -1 = ainda não achou a rota
    private Seguimento ultimoSeguimento = null;
    private double segundosParado = 0;

    public EstadoPatrulhando(Zumbi zumbi) {
        super(zumbi);
    }

    @Override
    public String getNome() {
        return "Patrulhando";
    }

    @Override
    public void enter() {
        segmento = acharEntradaNaRota();
    }

    @Override
    public void execute(World world) {
        Zumbi zumbi = getCharacter();

        // Se foi empurrado para um lugar de onde não vê mais a rota, procura a entrada de novo
        if (ultimoSeguimento != null && !zumbi.temLinhaDeVisaoAte(ultimoSeguimento.projecao())) {
            segmento = acharEntradaNaRota();
        }

        // Forças
        if (segmento >= 0) {
            double velocidadeDaRonda = zumbi.getMaxSpeed() * FRACAO_DA_VELOCIDADE_NA_RONDA;
            ultimoSeguimento = zumbi.pathFollowing(zumbi.getRota(), segmento, velocidadeDaRonda);
            segmento = ultimoSeguimento.segmento(); // pode ter avançado para o próximo segmento
            zumbi.aplicarForca(Comportamento.PATH_FOLLOWING, ultimoSeguimento.forca(), PESO_PATH_FOLLOWING);
        }
        zumbi.aplicarForca(Comportamento.SEPARATION, zumbi.separacaoDosOutrosZumbis(world), PESO_SEPARATION);

        segundosParado = zumbi.getSpeed() < VELOCIDADE_DE_QUEM_ESTA_PARADO ? segundosParado + world.getSecs() : 0;

        // Transições
        if (zumbi.deveSerControlado()) {
            zumbi.setState(new EstadoControlado(zumbi));
        } else if (zumbi.consegueVerOSobrevivente()) {
            zumbi.setState(new EstadoPerseguindo(zumbi));
        } else if (segmento < 0) {
            zumbi.setState(new EstadoVagando(zumbi)); // não vê a rota daqui
        } else if (segundosParado > SEGUNDOS_PARADO_PARA_DESISTIR) {
            zumbi.setState(new EstadoVagando(zumbi)); // travou de frente com outro zumbi
        }
    }

    /** Segmento da rota mais próximo que o zumbi consegue ver, ou -1 se não vê nenhum. */
    private int acharEntradaNaRota() {
        Zumbi zumbi = getCharacter();
        Caminho.Projecao entrada = zumbi.getRota().projecaoVisivelMaisProxima(
                zumbi.getPosition(), zumbi.getSimulacao().getMapa(), Zumbi.ALCANCE_PARA_ACHAR_A_ROTA);
        ultimoSeguimento = null;
        return entrada == null ? -1 : entrada.segmento();
    }

    /** Para o debug desenhar a previsão, a projeção e o alvo (pode ser null). */
    public Seguimento getUltimoSeguimento() {
        return ultimoSeguimento;
    }
}

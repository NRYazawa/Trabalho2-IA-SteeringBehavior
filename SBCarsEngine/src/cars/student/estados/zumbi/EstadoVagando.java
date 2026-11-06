package cars.student.estados.zumbi;

import cars.engine.World;
import cars.student.agentes.Comportamento;
import cars.student.agentes.Zumbi;
import cars.student.fsm.AbstractState;

/**
 * Zumbi automático sem rumo: WANDER + SEPARATION.
 *
 * Acontece depois de perder o sobrevivente de vista (fica "farejando" pela área) ou quando
 * ele não enxerga a rota de patrulha. Passado um tempo, se enxergar a rota, volta a patrulhar.
 */
public class EstadoVagando extends AbstractState<Zumbi> {

    private static final double PESO_WANDER = 1.0;
    private static final double PESO_SEPARATION = 1.5;
    private static final double SEGUNDOS_ANTES_DE_VOLTAR_A_ROTA = 5;

    private double segundosVagando = 0;

    public EstadoVagando(Zumbi zumbi) {
        super(zumbi);
    }

    @Override
    public String getNome() {
        return "Vagando";
    }

    @Override
    public void execute(World world) {
        Zumbi zumbi = getCharacter();
        segundosVagando += world.getSecs();

        // Forças
        zumbi.aplicarForca(Comportamento.WANDER, zumbi.wander(world.getSecs()), PESO_WANDER);
        zumbi.aplicarForca(Comportamento.SEPARATION, zumbi.separacaoDosOutrosZumbis(world), PESO_SEPARATION);

        // Transições
        if (zumbi.deveSerControlado()) {
            zumbi.setState(new EstadoControlado(zumbi));
        } else if (zumbi.consegueVerOSobrevivente()) {
            zumbi.setState(new EstadoPerseguindo(zumbi));
        } else if (segundosVagando > SEGUNDOS_ANTES_DE_VOLTAR_A_ROTA && zumbi.enxergaARota()) {
            zumbi.setState(new EstadoPatrulhando(zumbi));
        }
    }
}

package cars.student.estados.zumbi;

import cars.engine.Vector2;
import cars.engine.World;
import cars.student.agentes.Comportamento;
import cars.student.agentes.Zumbi;
import cars.student.fsm.AbstractState;

import static cars.engine.Vector2.distance;

/**
 * Viu o sobrevivente: Seek nele. Se perder de vista (ele virou num corredor),
 * vai com Arrive até o último lugar onde o viu. Chegou lá (ou cansou de procurar)
 * e não achou ninguém: volta a vagar.
 */
public class EstadoPerseguindo extends AbstractState<Zumbi> {

    private static final double PESO_SEPARATION = 1.5;
    private static final double RAIO_DE_FRENAGEM = 40;
    private static final double DISTANCIA_PARA_DESISTIR = 10;
    private static final double SEGUNDOS_PARA_DESISTIR = 4;

    private Vector2 ultimaPosicaoVista;
    private double segundosSemVer = 0;

    public EstadoPerseguindo(Zumbi zumbi) {
        super(zumbi);
    }

    @Override
    public String getNome() {
        return "Perseguindo";
    }

    @Override
    public void enter() {
        ultimaPosicaoVista = getCharacter().getSimulacao().getSobrevivente().getPosition();
    }

    @Override
    public void execute(World world) {
        Zumbi zumbi = getCharacter();
        boolean estaVendo = zumbi.consegueVerOSobrevivente();

        // Forças
        if (estaVendo) {
            ultimaPosicaoVista = zumbi.getSimulacao().getSobrevivente().getPosition();
            segundosSemVer = 0;
            zumbi.aplicarForca(Comportamento.SEEK, zumbi.seek(ultimaPosicaoVista), 1.0);
        } else {
            zumbi.aplicarForca(Comportamento.ARRIVE, zumbi.arrive(ultimaPosicaoVista, RAIO_DE_FRENAGEM), 1.0);
            segundosSemVer += world.getSecs();
        }
        zumbi.aplicarForca(Comportamento.SEPARATION, zumbi.separacaoDosOutrosZumbis(world), PESO_SEPARATION);

        // Transições
        if (zumbi.deveSerControlado()) {
            zumbi.setState(new EstadoControlado(zumbi));
        } else if (!estaVendo && desistiuDeProcurar(zumbi)) {
            zumbi.setState(new EstadoVagando(zumbi));
        }
    }

    private boolean desistiuDeProcurar(Zumbi zumbi) {
        boolean chegou = distance(zumbi.getPosition(), ultimaPosicaoVista) < DISTANCIA_PARA_DESISTIR;
        return chegou || segundosSemVer > SEGUNDOS_PARA_DESISTIR;
    }
}

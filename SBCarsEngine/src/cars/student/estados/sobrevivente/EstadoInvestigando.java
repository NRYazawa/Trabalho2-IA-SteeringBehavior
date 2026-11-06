package cars.student.estados.sobrevivente;

import cars.engine.World;
import cars.student.Item;
import cars.student.agentes.Comportamento;
import cars.student.agentes.Sobrevivente;
import cars.student.fsm.AbstractState;

/** Parado em cima do item, um timer corre enquanto ele investiga e guarda na bolsa. */
public class EstadoInvestigando extends AbstractState<Sobrevivente> {

    private static final double RAIO_DE_FRENAGEM = 30;

    private final Item item;
    private double tempoRestante;

    public EstadoInvestigando(Sobrevivente sobrevivente, Item item) {
        super(sobrevivente);
        this.item = item;
    }

    @Override
    public String getNome() {
        return "Investigando item";
    }

    @Override
    public void enter() {
        tempoRestante = getCharacter().getPersonalidade().getTempoDeInvestigacao();
    }

    @Override
    public void execute(World world) {
        Sobrevivente sobrevivente = getCharacter();

        // Forças: Arrive no próprio item = ficar parado em cima dele
        sobrevivente.aplicarForca(Comportamento.ARRIVE, sobrevivente.arrive(item.getPosicao(), RAIO_DE_FRENAGEM), 1.0);
        tempoRestante -= world.getSecs();

        // Transições (um zumbi pode interromper a investigação)
        switch (sobrevivente.decidirIntencao()) {
            case LUTAR -> sobrevivente.setState(new EstadoAtirando(sobrevivente));
            case FUGIR -> sobrevivente.setState(new EstadoFugindo(sobrevivente));
            case LOOTEAR -> {
                if (tempoRestante <= 0) {
                    sobrevivente.guardarNaBolsa(item);
                    sobrevivente.setState(new EstadoProcurando(sobrevivente));
                }
            }
        }
    }

    /** De 0 (começou) a 1 (terminou), para a barrinha de progresso. */
    public double getProgresso() {
        double total = getCharacter().getPersonalidade().getTempoDeInvestigacao();
        return Math.clamp(1 - tempoRestante / total, 0.0, 1.0);
    }
}

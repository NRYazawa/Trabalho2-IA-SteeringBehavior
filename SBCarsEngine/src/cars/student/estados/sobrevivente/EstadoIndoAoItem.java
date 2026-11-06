package cars.student.estados.sobrevivente;

import cars.engine.World;
import cars.student.Item;
import cars.student.agentes.Comportamento;
import cars.student.agentes.Sobrevivente;
import cars.student.fsm.AbstractState;

/** Viu um item: vai até ele com Arrive, desacelerando para parar em cima. */
public class EstadoIndoAoItem extends AbstractState<Sobrevivente> {

    private static final double RAIO_DE_FRENAGEM = 50;
    private static final double DISTANCIA_PARA_PEGAR = 8;

    private final Item item;

    public EstadoIndoAoItem(Sobrevivente sobrevivente, Item item) {
        super(sobrevivente);
        this.item = item;
    }

    @Override
    public String getNome() {
        return "Indo ao item";
    }

    @Override
    public void execute(World world) {
        Sobrevivente sobrevivente = getCharacter();

        // Forças
        sobrevivente.aplicarForca(Comportamento.ARRIVE, sobrevivente.arrive(item.getPosicao(), RAIO_DE_FRENAGEM), 1.0);

        // Transições
        switch (sobrevivente.decidirIntencao()) {
            case LUTAR -> sobrevivente.setState(new EstadoAtirando(sobrevivente));
            case FUGIR -> sobrevivente.setState(new EstadoFugindo(sobrevivente));
            case LOOTEAR -> {
                if (!sobrevivente.consegueVer(item.getPosicao(), Sobrevivente.RAIO_DE_VISAO)) {
                    sobrevivente.setState(new EstadoProcurando(sobrevivente)); // perdeu o item de vista
                } else if (sobrevivente.distanciaAte(item.getPosicao()) < DISTANCIA_PARA_PEGAR) {
                    sobrevivente.setState(new EstadoInvestigando(sobrevivente, item));
                }
            }
        }
    }
}

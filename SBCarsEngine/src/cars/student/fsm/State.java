package cars.student.fsm;

import cars.engine.World;

/**
 * Contrato de um estado (padrão State), igual ao visto em aula:
 * enter() → execute() a cada frame → leave().
 *
 * @param <C> tipo do personagem (o "contexto") que usa este estado
 */
public interface State<C> {
    C getCharacter();

    /** Nome curto, mostrado no console e no debug. */
    String getNome();

    /** Chamado uma vez, ao entrar no estado. */
    void enter();

    /**
     * Chamado a cada frame. O estado faz duas coisas:
     * 1) escolhe quais comportamentos de steering o personagem usa (somando forças nele);
     * 2) decide se é hora de trocar de estado.
     */
    void execute(World world);

    /** Chamado uma vez, ao sair do estado. */
    void leave();
}

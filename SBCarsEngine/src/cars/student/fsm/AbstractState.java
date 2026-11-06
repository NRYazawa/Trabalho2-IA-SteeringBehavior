package cars.student.fsm;

/**
 * Base dos estados concretos: guarda o personagem e deixa enter/leave opcionais.
 *
 * @param <C> tipo do personagem (o "contexto") que usa este estado
 */
public abstract class AbstractState<C> implements State<C> {
    private final C character;

    protected AbstractState(C character) {
        this.character = character;
    }

    @Override
    public C getCharacter() {
        return character;
    }

    @Override
    public void enter() {
    }

    @Override
    public void leave() {
    }
}

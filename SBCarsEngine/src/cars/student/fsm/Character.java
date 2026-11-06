package cars.student.fsm;

/**
 * Contrato de todo personagem com máquina de estados (Sobrevivente e Zumbi).
 *
 * O "update()" visto em aula aqui é o calculateSteering() da engine:
 * ela chama esse método a cada frame e ele executa o estado atual.
 *
 * @param <C> o próprio tipo do personagem (ex.: Character&lt;Zumbi&gt;)
 */
public interface Character<C> {
    void setState(State<C> state);

    State<C> getState();

    void printStats(String mensagem);
}

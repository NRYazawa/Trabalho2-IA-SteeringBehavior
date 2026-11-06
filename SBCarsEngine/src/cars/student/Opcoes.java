package cars.student;

/**
 * Opções alteradas pela janela de debug.
 * São "volatile" porque a janela de debug (thread do Swing) escreve
 * e o loop do jogo (outra thread) lê.
 */
public class Opcoes {

    public enum ModoDeJogo {
        /** Você controla um zumbi com as setas e o mouse. */
        TESTE,
        /** Tela escura com lanterna; todos os zumbis andam sozinhos. */
        SOBREVIVENTE
    }

    public volatile ModoDeJogo modo = ModoDeJogo.TESTE;
    public volatile boolean modoAutomatico = false;
    public volatile boolean mostrarVisao = true;
    public volatile boolean mostrarForcas = true;
    public volatile boolean mostrarAntenas = true;
    public volatile boolean mostrarRota = true;
    public volatile boolean mostrarEstados = true;
}

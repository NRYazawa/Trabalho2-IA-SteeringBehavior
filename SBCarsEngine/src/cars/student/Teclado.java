package cars.student;

import cars.engine.Vector2;

import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.event.KeyEvent;

import static cars.engine.Vector2.normalize;
import static cars.engine.Vector2.vec2;

/**
 * Lê as setas (ou WASD) em qualquer janela do programa,
 * inclusive quando a janela de debug está em foco.
 */
public class Teclado implements KeyEventDispatcher {

    private volatile boolean cima, baixo, esquerda, direita;

    public void registrar() {
        KeyboardFocusManager gerenciador = KeyboardFocusManager.getCurrentKeyboardFocusManager();
        gerenciador.addKeyEventDispatcher(this);
        // Se o programa perder o foco com uma tecla apertada, considera todas soltas
        gerenciador.addPropertyChangeListener("activeWindow", evento -> {
            if (evento.getNewValue() == null) cima = baixo = esquerda = direita = false;
        });
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent evento) {
        boolean apertou = evento.getID() == KeyEvent.KEY_PRESSED;
        boolean soltou = evento.getID() == KeyEvent.KEY_RELEASED;
        if (!apertou && !soltou) return false;

        switch (evento.getKeyCode()) {
            case KeyEvent.VK_UP, KeyEvent.VK_W -> cima = apertou;
            case KeyEvent.VK_DOWN, KeyEvent.VK_S -> baixo = apertou;
            case KeyEvent.VK_LEFT, KeyEvent.VK_A -> esquerda = apertou;
            case KeyEvent.VK_RIGHT, KeyEvent.VK_D -> direita = apertou;
        }
        return false; // não "consome" a tecla
    }

    /** Direção pedida pelas teclas (vetor unitário) ou zero se nada apertado. */
    public Vector2 direcao() {
        double x = (direita ? 1 : 0) - (esquerda ? 1 : 0);
        double y = (baixo ? 1 : 0) - (cima ? 1 : 0);
        return normalize(vec2(x, y));
    }
}

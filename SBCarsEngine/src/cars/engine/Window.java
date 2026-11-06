package cars.engine;

import cars.student.Setup;
import cars.student.Simulacao;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;

/**
 * Janela principal e loop do jogo.
 *
 * [ALTERAÇÃO DO ALUNO] A janela agora só cuida do "motor": abre a tela, roda o loop
 * a ~60 FPS, atualiza os carros e repassa o desenho para a {@link Simulacao}.
 * Toda a lógica do trabalho fica no pacote cars.student.
 */
public final class Window extends JFrame implements Runnable {
    public static final int INITIAL_WIDTH = 1024;
    public static final int INITIAL_HEIGHT = 768;

    /** Intervalo entre frames (~60 FPS). */
    private static final long FRAME_MILLIS = 16;

    /** Limita o passo de tempo: se a janela travar (ex.: arrastando), ninguém "teleporta" através da parede. */
    private static final double MAX_SECS_PER_FRAME = 0.05;

    private final Simulacao simulacao;
    // "volatile": escritos pela thread do Swing (mouse) e lidos pelo loop do jogo
    private volatile Vector2 clickPos = null;
    private volatile Vector2 mousePos = null;

    private Window(Simulacao simulacao) {
        super("Steering Behaviors - Sobrevivente vs Zumbis");
        this.simulacao = simulacao;

        setSize(INITIAL_WIDTH, INITIAL_HEIGHT);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setIgnoreRepaint(true);
        setResizable(false);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                start();
            }
        });

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                // posição do clique relativa ao centro da janela (mesmo sistema de coordenadas do mundo)
                clickPos = new Vector2(e.getX() - getWidth() / 2.0, e.getY() - getHeight() / 2.0);
            }
        });

        addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                mousePos = new Vector2(e.getX() - getWidth() / 2.0, e.getY() - getHeight() / 2.0);
            }
        });
    }

    public static void main(String[] args) {
        EventQueue.invokeLater(() -> {
            // Primeiro o usuário escolhe o personagem; só depois a janela abre.
            Simulacao simulacao = new Setup().criarSimulacao();
            new Window(simulacao).setVisible(true);
        });
    }

    private void start() {
        createBufferStrategy(3);
        final var gameLoop = new Thread(this, "game-loop");
        gameLoop.setDaemon(true);
        gameLoop.start();
    }

    @Override
    public void run() {
        long prev = System.nanoTime();
        try {
            final var strategy = getBufferStrategy();
            while (true) {
                final long now = System.nanoTime();
                final double secs = Math.min((now - prev) / 1_000_000_000.0, MAX_SECS_PER_FRAME);
                prev = now;

                try {
                    update(secs);

                    final var g2d = (Graphics2D) strategy.getDrawGraphics();
                    draw(g2d);
                    g2d.dispose();
                    strategy.show();
                    Toolkit.getDefaultToolkit().sync(); // helps on some systems
                } catch (RuntimeException e) {
                    e.printStackTrace(); // mostra o erro sem congelar o jogo
                }

                Thread.sleep(FRAME_MILLIS);
            }
        } catch (InterruptedException e) {
            System.err.println("Interrupted");
        }
        System.exit(0);
    }

    private void update(final double secs) {
        if (simulacao.terminou()) return;

        // Cada clique vale por um frame só: depois de entregue, é apagado
        final Vector2 click = clickPos;
        clickPos = null;

        // 1) A engine move cada carro usando o steering que ele calcula
        final List<Car> cars = simulacao.getCarros();
        for (Car car : cars) {
            car.update(new World(secs, car, cars, mousePos, click, getWidth(), getHeight()));
        }

        // 2) Depois, as regras do jogo (tiros, mordidas, respawn, itens)
        simulacao.atualizar(secs);
    }

    private void draw(Graphics2D g2d) {
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2d.translate(getWidth() / 2.0, getHeight() / 2.0); // origem (0,0) no centro da janela
        simulacao.desenhar(g2d, getWidth(), getHeight());
    }
}

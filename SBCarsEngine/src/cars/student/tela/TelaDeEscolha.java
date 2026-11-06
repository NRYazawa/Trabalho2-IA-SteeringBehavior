package cars.student.tela;

import cars.student.agentes.Intencao;
import cars.student.agentes.Personalidade;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;

/**
 * Tela inicial: um cartão por tipo de sobrevivente, com os pesos em barras.
 * Clicar no cartão escolhe o personagem e fecha a tela.
 */
public class TelaDeEscolha extends JDialog {

    private static final Color FUNDO = new Color(43, 45, 49);
    private static final Color FUNDO_DO_CARTAO = new Color(58, 61, 67);
    private static final Color TEXTO = new Color(235, 235, 235);
    private static final Color TEXTO_FRACO = new Color(170, 172, 178);

    private Personalidade escolhida = null;

    /** Abre a tela e espera a escolha. Devolve null se a janela for fechada. */
    public static Personalidade escolher() {
        TelaDeEscolha tela = new TelaDeEscolha();
        tela.setVisible(true); // modal: só continua depois que fechar
        return tela.escolhida;
    }

    private TelaDeEscolha() {
        super((Frame) null, "Escolha o sobrevivente", true);

        JPanel conteudo = new JPanel(new BorderLayout(0, 18));
        conteudo.setBackground(FUNDO);
        conteudo.setBorder(BorderFactory.createEmptyBorder(24, 24, 20, 24));

        conteudo.add(cabecalho(), BorderLayout.NORTH);
        conteudo.add(cartoes(), BorderLayout.CENTER);
        conteudo.add(rotulo("Modo Teste e Modo Sobrevivente podem ser trocados depois, na janela Debug.",
                12, Font.PLAIN, TEXTO_FRACO), BorderLayout.SOUTH);

        setContentPane(conteudo);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setResizable(false);
        pack();
        setLocationRelativeTo(null);
    }

    private JComponent cabecalho() {
        JPanel cabecalho = new JPanel(new GridLayout(2, 1, 0, 4));
        cabecalho.setOpaque(false);
        cabecalho.add(rotulo("Sobrevivente vs Zumbis", 26, Font.BOLD, TEXTO));
        cabecalho.add(rotulo("Escolha quem vai tentar sobreviver no escritório", 14, Font.PLAIN, TEXTO_FRACO));
        return cabecalho;
    }

    private JComponent cartoes() {
        JPanel cartoes = new JPanel(new GridLayout(1, 0, 16, 0));
        cartoes.setOpaque(false);
        for (Personalidade personalidade : Personalidade.values()) {
            cartoes.add(new Cartao(personalidade));
        }
        return cartoes;
    }

    private static JLabel rotulo(String texto, int tamanho, int estilo, Color cor) {
        JLabel rotulo = new JLabel(texto);
        rotulo.setFont(new Font("SansSerif", estilo, tamanho));
        rotulo.setForeground(cor);
        return rotulo;
    }

    // =====================================================================
    //  CARTÃO DE UM PERSONAGEM
    // =====================================================================

    private class Cartao extends JPanel {
        private final Personalidade personalidade;
        private final JLabel convite;
        private boolean mouseEmCima = false;

        Cartao(Personalidade personalidade) {
            this.personalidade = personalidade;
            setOpaque(false);
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));
            setPreferredSize(new Dimension(250, 350));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            add(new Avatar(personalidade.getCor()));
            add(Box.createVerticalStrut(10));
            add(rotulo(personalidade.getNome(), 20, Font.BOLD, TEXTO));
            add(Box.createVerticalStrut(6));
            add(rotulo("<html><body style='width:165px'>" + personalidade.getFrase() + "</body></html>", 12, Font.PLAIN, TEXTO_FRACO));
            add(Box.createVerticalStrut(14));
            for (Intencao intencao : Intencao.values()) {
                add(new BarraDePeso(intencao.getNome(), personalidade.getPeso(intencao), personalidade.getCor()));
                add(Box.createVerticalStrut(6));
            }
            add(Box.createVerticalStrut(8));
            add(rotulo(String.format("Recua a %.0f px · foge até %.0f px",
                    personalidade.getDistanciaMinima(), personalidade.getDistanciaSegura()), 11, Font.PLAIN, TEXTO_FRACO));
            add(rotulo(String.format("Investiga um item em %.1f s", personalidade.getTempoDeInvestigacao()),
                    11, Font.PLAIN, TEXTO_FRACO));
            add(rotulo("No empate: " + personalidade.getOrdemDeEnfase().get(0).getNome(), 11, Font.PLAIN, TEXTO_FRACO));
            add(Box.createVerticalGlue());
            convite = rotulo("Clique para jogar  ▶", 14, Font.BOLD, personalidade.getCor().brighter());
            convite.setVisible(false); // só aparece com o mouse em cima
            add(convite);

            for (Component filho : getComponents()) {
                if (filho instanceof JComponent componente) componente.setAlignmentX(LEFT_ALIGNMENT);
            }

            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { destacar(true); }
                @Override public void mouseExited(MouseEvent e) { destacar(false); }
                @Override public void mouseClicked(MouseEvent e) {
                    escolhida = personalidade;
                    dispose();
                }
            });
        }

        private void destacar(boolean destacado) {
            mouseEmCima = destacado;
            convite.setVisible(destacado);
            repaint();
        }

        /** Fundo arredondado; com o mouse em cima, ganha borda da cor do personagem. */
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            RoundRectangle2D fundo = new RoundRectangle2D.Double(1, 1, getWidth() - 3, getHeight() - 3, 18, 18);
            g2.setColor(mouseEmCima ? FUNDO_DO_CARTAO.brighter() : FUNDO_DO_CARTAO);
            g2.fill(fundo);
            if (mouseEmCima) {
                g2.setColor(personalidade.getCor());
                g2.setStroke(new BasicStroke(3f));
                g2.draw(fundo);
            }
            g2.dispose();
        }
    }

    /** O mesmo quadradinho com "olhos" do jogo, só que maior. */
    private static class Avatar extends JComponent {
        private final Color cor;

        Avatar(Color cor) {
            this.cor = cor;
            setPreferredSize(new Dimension(56, 56));
            setMaximumSize(new Dimension(56, 56));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(cor);
            g2.fillRoundRect(4, 4, 48, 48, 8, 8);
            g2.setColor(cor.darker().darker());
            g2.setStroke(new BasicStroke(3f));
            g2.drawRoundRect(4, 4, 48, 48, 8, 8);
            g2.setColor(Color.BLACK);
            g2.fillRect(38, 14, 6, 9);
            g2.fillRect(38, 33, 6, 9);
            g2.dispose();
        }
    }

    /** "Lutar  ███████░░░  1.0" */
    private static class BarraDePeso extends JComponent {
        private final String nome;
        private final double peso;
        private final Color cor;

        BarraDePeso(String nome, double peso, Color cor) {
            this.nome = nome;
            this.peso = peso;
            this.cor = cor;
            setPreferredSize(new Dimension(214, 16));
            setMaximumSize(new Dimension(214, 16));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setFont(new Font("SansSerif", Font.BOLD, 12));
            g2.setColor(TEXTO);
            g2.drawString(nome, 0, 12);

            int x = 62, largura = 120;
            g2.setColor(new Color(35, 36, 40));
            g2.fillRoundRect(x, 3, largura, 10, 10, 10);
            g2.setColor(cor);
            g2.fillRoundRect(x, 3, (int) (largura * peso), 10, 10, 10);

            g2.setColor(TEXTO);
            g2.drawString(String.format("%.1f", peso), x + largura + 8, 12);
            g2.dispose();
        }
    }
}

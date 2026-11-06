package cars.student.tela;

import cars.student.Simulacao;
import cars.student.agentes.Agente;
import cars.student.agentes.Agente.ForcaAplicada;
import cars.student.agentes.Comportamento;
import cars.student.agentes.Intencao;
import cars.student.agentes.Sobrevivente;
import cars.student.agentes.Zumbi;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Map;

import static cars.student.agentes.Comportamento.*;

/**
 * Pedaço da janela de Debug que mostra, ao vivo, o que está guiando os agentes:
 *  - Sobrevivente: cada força com o peso usado e a intensidade, a resultante e as vontades;
 *  - Zumbi em foco (o seu, ou o mais próximo do sobrevivente): as forças e a resultante.
 *
 * Roda na thread do Swing, então só lê as cópias "volatile" que os agentes guardam a cada frame.
 */
public class PainelAoVivo extends JComponent {

    private static final int ALTURA_DA_LINHA = 18;
    private static final int X_DO_PESO = 196;
    private static final int X_DA_BARRA = 232;
    private static final int LARGURA_DA_BARRA = 84;
    private static final Color INATIVO = new Color(170, 170, 170);

    /** Comportamentos que cada tipo de agente pode usar (linha cinza = não usado neste frame). */
    private static final List<Comportamento> DO_SOBREVIVENTE = List.of(SEEK, FLEE, ARRIVE, WANDER, OBSTACLE_AVOIDANCE);
    private static final List<Comportamento> DO_ZUMBI = List.of(SEEK, ARRIVE, WANDER, SEPARATION, OBSTACLE_AVOIDANCE, PATH_FOLLOWING, CONTROLE);

    private final Simulacao simulacao;

    public PainelAoVivo(Simulacao simulacao) {
        this.simulacao = simulacao;
        int linhas = 2 * 3 + DO_SOBREVIVENTE.size() + DO_ZUMBI.size() + 1 + Intencao.values().length + 2;
        setPreferredSize(new Dimension(368, linhas * ALTURA_DA_LINHA + 24));
    }

    /** Começa a redesenhar 10 vezes por segundo (chamado depois de montar a janela). */
    public void iniciar() {
        new Timer(100, e -> repaint()).start();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setFont(new Font("SansSerif", Font.PLAIN, 12));

        Sobrevivente sobrevivente = simulacao.getSobrevivente();
        int y = ALTURA_DA_LINHA;
        y = desenharForcas(g2, y, "SOBREVIVENTE", sobrevivente, DO_SOBREVIVENTE);
        y = desenharVontades(g2, y, sobrevivente);

        Zumbi zumbi = simulacao.getZumbiEmFoco();
        if (zumbi != null) {
            String titulo = zumbi.isDoJogador() ? "ZUMBI (o seu)" : "ZUMBI (o mais próximo)";
            desenharForcas(g2, y + 8, titulo, zumbi, DO_ZUMBI);
        }
        g2.dispose();
    }

    /** Título com o estado, uma linha por comportamento (peso e intensidade) e a resultante. */
    private int desenharForcas(Graphics2D g2, int y, String titulo, Agente agente, List<Comportamento> comportamentos) {
        g2.setColor(Color.BLACK);
        g2.setFont(g2.getFont().deriveFont(Font.BOLD));
        g2.drawString(titulo + " — " + agente.getNomeDoEstado(), 0, y);
        y += ALTURA_DA_LINHA;

        g2.setColor(Color.DARK_GRAY);
        g2.drawString("Força", 0, y);
        g2.drawString("peso", X_DO_PESO, y);
        g2.drawString("intensidade", X_DA_BARRA, y);
        y += ALTURA_DA_LINHA;
        g2.setFont(g2.getFont().deriveFont(Font.PLAIN));

        double forcaMaxima = agente.getMaxForce();
        List<ForcaAplicada> forcas = agente.getUltimasForcas();
        for (Comportamento comportamento : comportamentos) {
            ForcaAplicada forca = procurar(forcas, comportamento);
            Color cor = forca == null ? INATIVO : comportamento.getCor();

            g2.setColor(cor);
            g2.fillRect(0, y - 9, 9, 9);
            g2.drawString(comportamento.getNome(), 14, y);
            g2.drawString(forca == null ? "-" : String.format("%.2f", forca.peso()), X_DO_PESO, y);
            if (forca != null) desenharBarra(g2, y, forca.intensidade() / (2 * forcaMaxima), cor,
                    String.format("%.0f", forca.intensidade()));
            y += ALTURA_DA_LINHA;
        }

        double resultante = agente.getIntensidadeDaUltimaResultante();
        g2.setColor(Renderizador.COR_RESULTANTE);
        g2.setFont(g2.getFont().deriveFont(Font.BOLD));
        g2.drawString("RESULTANTE (máx " + (int) forcaMaxima + ")", 0, y);
        desenharBarra(g2, y, resultante / (2 * forcaMaxima), Renderizador.COR_RESULTANTE, String.format("%.0f", resultante));
        g2.setFont(g2.getFont().deriveFont(Font.PLAIN));
        return y + ALTURA_DA_LINHA + 6;
    }

    /** Vontades do sobrevivente (peso da personalidade × estímulo), com a vencedora marcada. */
    private int desenharVontades(Graphics2D g2, int y, Sobrevivente sobrevivente) {
        g2.setColor(Color.BLACK);
        g2.setFont(g2.getFont().deriveFont(Font.BOLD));
        g2.drawString("Vontade (peso × estímulo)", 0, y);
        y += ALTURA_DA_LINHA;

        Map<Intencao, Double> vontades = sobrevivente.getUltimasVontades();
        Intencao decisao = sobrevivente.getUltimaDecisao();
        for (Intencao intencao : Intencao.values()) {
            boolean venceu = intencao == decisao;
            double vontade = vontades.getOrDefault(intencao, 0.0);
            g2.setFont(g2.getFont().deriveFont(venceu ? Font.BOLD : Font.PLAIN));
            g2.setColor(Color.BLACK);
            g2.drawString((venceu ? "► " : "   ") + intencao.getNome(), 0, y);
            g2.drawString(String.format("%.1f ×", sobrevivente.getPersonalidade().getPeso(intencao)), X_DO_PESO, y);
            desenharBarra(g2, y, vontade, venceu ? new Color(230, 160, 0) : INATIVO, String.format("%.2f", vontade));
            y += ALTURA_DA_LINHA;
        }
        g2.setFont(g2.getFont().deriveFont(Font.PLAIN));
        return y;
    }

    private void desenharBarra(Graphics2D g2, int y, double fracao, Color cor, String valor) {
        g2.setColor(new Color(225, 225, 225));
        g2.fillRect(X_DA_BARRA, y - 9, LARGURA_DA_BARRA, 9);
        g2.setColor(cor);
        g2.fillRect(X_DA_BARRA, y - 9, (int) (LARGURA_DA_BARRA * Math.clamp(fracao, 0.0, 1.0)), 9);
        g2.setColor(Color.BLACK);
        g2.drawString(valor, X_DA_BARRA + LARGURA_DA_BARRA + 6, y);
    }

    private static ForcaAplicada procurar(List<ForcaAplicada> forcas, Comportamento comportamento) {
        for (ForcaAplicada forca : forcas) {
            if (forca.comportamento() == comportamento) return forca;
        }
        return null;
    }
}

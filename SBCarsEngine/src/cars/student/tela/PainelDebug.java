package cars.student.tela;

import cars.student.Opcoes;
import cars.student.Opcoes.ModoDeJogo;
import cars.student.Simulacao;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;

/** Janela separada com as opções de debug e os pesos das forças ao vivo. */
public class PainelDebug extends JFrame {

    private final Opcoes opcoes;

    public PainelDebug(Simulacao simulacao) {
        super("Debug");
        this.opcoes = simulacao.getOpcoes();

        JPanel conteudo = new JPanel();
        conteudo.setLayout(new BoxLayout(conteudo, BoxLayout.Y_AXIS));
        conteudo.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        conteudo.add(secaoModoDeJogo());
        conteudo.add(secaoVisualizacao());
        conteudo.add(secaoZumbis(simulacao));
        conteudo.add(secaoAoVivo(simulacao));

        add(conteudo);
        pack();
        setResizable(false);
        setDefaultCloseOperation(HIDE_ON_CLOSE);
        posicionarNoCantoDireitoDaTela();
    }

    private JPanel secaoModoDeJogo() {
        JRadioButton teste = new JRadioButton("Modo Teste (você controla um zumbi)", opcoes.modo == ModoDeJogo.TESTE);
        JRadioButton sobrevivente = new JRadioButton("Modo Sobrevivente (escuro + lanterna)", opcoes.modo == ModoDeJogo.SOBREVIVENTE);
        teste.addActionListener(e -> opcoes.modo = ModoDeJogo.TESTE);
        sobrevivente.addActionListener(e -> opcoes.modo = ModoDeJogo.SOBREVIVENTE);

        ButtonGroup grupo = new ButtonGroup();
        grupo.add(teste);
        grupo.add(sobrevivente);
        return secao("Modo de jogo", teste, sobrevivente);
    }

    private JPanel secaoVisualizacao() {
        return secao("Visualização",
                caixa("Visão e distâncias (círculos)", opcoes.mostrarVisao, marcado -> opcoes.mostrarVisao = marcado),
                caixa("Vetores de força", opcoes.mostrarForcas, marcado -> opcoes.mostrarForcas = marcado),
                caixa("Antenas do Obstacle Avoidance", opcoes.mostrarAntenas, marcado -> opcoes.mostrarAntenas = marcado),
                caixa("Rota de patrulha (Path Following)", opcoes.mostrarRota, marcado -> opcoes.mostrarRota = marcado),
                caixa("Estados e vontades", opcoes.mostrarEstados, marcado -> opcoes.mostrarEstados = marcado));
    }

    private JPanel secaoZumbis(Simulacao simulacao) {
        JCheckBox automatico = caixa("Modo automático (seu zumbi anda sozinho)", opcoes.modoAutomatico,
                marcado -> opcoes.modoAutomatico = marcado);

        JButton adicionar = new JButton("Adicionar zumbi");
        adicionar.setFocusable(false);
        adicionar.addActionListener(e -> simulacao.pedirNovoZumbi());
        return secao("Zumbis", automatico, adicionar);
    }

    private JPanel secaoAoVivo(Simulacao simulacao) {
        JLabel legenda = new JLabel("No mapa: seta azul = velocidade · seta preta = resultante");
        legenda.setFont(legenda.getFont().deriveFont(11f));
        PainelAoVivo aoVivo = new PainelAoVivo(simulacao);
        aoVivo.iniciar();
        return secao("Ao vivo: forças, pesos e vontades", aoVivo, legenda);
    }

    // ---------------------------------------------------------------------
    //  Pequenos ajudantes de layout
    // ---------------------------------------------------------------------

    private static JPanel secao(String titulo, JComponent... componentes) {
        JPanel painel = new JPanel();
        painel.setLayout(new BoxLayout(painel, BoxLayout.Y_AXIS));
        painel.setBorder(BorderFactory.createTitledBorder(titulo));
        painel.setAlignmentX(LEFT_ALIGNMENT);
        for (JComponent componente : componentes) {
            componente.setFocusable(false); // o foco fica livre para as setas
            componente.setAlignmentX(LEFT_ALIGNMENT);
            painel.add(componente);
        }
        return painel;
    }

    private static JCheckBox caixa(String texto, boolean marcadoNoInicio, Consumer<Boolean> aoMudar) {
        JCheckBox caixa = new JCheckBox(texto, marcadoNoInicio);
        caixa.addActionListener(e -> aoMudar.accept(caixa.isSelected()));
        return caixa;
    }

    private void posicionarNoCantoDireitoDaTela() {
        Rectangle tela = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        setLocation(tela.x + tela.width - getWidth() - 10, tela.y + 40);
    }
}

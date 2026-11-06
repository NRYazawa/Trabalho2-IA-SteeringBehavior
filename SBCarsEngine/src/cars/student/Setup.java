package cars.student;

import cars.student.agentes.Personalidade;
import cars.student.tela.PainelDebug;
import cars.student.tela.TelaDeEscolha;

import javax.swing.SwingUtilities;

/**
 * Ponto de entrada do aluno (chamado pela Window antes de abrir o jogo):
 * escolhe o tipo de sobrevivente, cria a simulação e abre a janela de debug.
 */
public class Setup {

    public Simulacao criarSimulacao() {
        Personalidade personalidade = TelaDeEscolha.escolher();
        if (personalidade == null) System.exit(0); // fechou a tela sem escolher

        Simulacao simulacao = new Simulacao(personalidade);
        simulacao.getTeclado().registrar();

        // Abre a janela de debug logo depois da janela principal, para ela ficar por cima
        PainelDebug painelDebug = new PainelDebug(simulacao);
        SwingUtilities.invokeLater(() -> painelDebug.setVisible(true));
        return simulacao;
    }
}

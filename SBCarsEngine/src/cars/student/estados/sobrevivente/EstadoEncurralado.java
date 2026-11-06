package cars.student.estados.sobrevivente;

import cars.engine.World;
import cars.student.agentes.Comportamento;
import cars.student.agentes.Sobrevivente;
import cars.student.agentes.Zumbi;
import cars.student.fsm.AbstractState;

import java.util.List;

/**
 * Queria fugir, mas todo caminho que ele vê passa perto do zumbi: está encurralado.
 * Fica firme no lugar, encara o zumbi mais próximo e atira em desespero.
 * De tempos em tempos procura de novo uma saída.
 */
public class EstadoEncurralado extends AbstractState<Sobrevivente> {

    private static final double RAIO_DE_FRENAGEM = 30;
    private static final double SEGUNDOS_ENTRE_TENTATIVAS = 0.8;

    private double segundosDesdeAUltimaTentativa = 0;

    public EstadoEncurralado(Sobrevivente sobrevivente) {
        super(sobrevivente);
    }

    @Override
    public String getNome() {
        return "Encurralado!";
    }

    @Override
    public void execute(World world) {
        Sobrevivente sobrevivente = getCharacter();
        double distanciaSegura = sobrevivente.getPersonalidade().getDistanciaSegura();
        List<Zumbi> ameacas = sobrevivente.zumbisVisiveis(distanciaSegura);

        // Transição: o perigo passou (ou ele matou o zumbi)
        if (ameacas.isEmpty()) {
            sobrevivente.setState(new EstadoProcurando(sobrevivente));
            return;
        }

        // Forças: Arrive na própria posição = ficar firme no lugar
        sobrevivente.aplicarForca(Comportamento.ARRIVE, sobrevivente.arrive(sobrevivente.getPosition(), RAIO_DE_FRENAGEM), 1.0);

        // Ação: atirar no zumbi mais próximo
        sobrevivente.atirarEm(sobrevivente.zumbiMaisProximoVisivel(distanciaSegura));

        // Transição: abriu uma saída
        segundosDesdeAUltimaTentativa += world.getSecs();
        if (segundosDesdeAUltimaTentativa > SEGUNDOS_ENTRE_TENTATIVAS) {
            segundosDesdeAUltimaTentativa = 0;
            if (sobrevivente.escolherPontoDeFuga(ameacas) != null) {
                sobrevivente.setState(new EstadoFugindo(sobrevivente));
            }
        }
    }
}

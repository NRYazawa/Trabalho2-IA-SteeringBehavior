package cars.student.estados.sobrevivente;

import cars.engine.Vector2;
import cars.engine.World;
import cars.student.agentes.Comportamento;
import cars.student.agentes.Sobrevivente;
import cars.student.agentes.Zumbi;
import cars.student.fsm.AbstractState;

import java.util.List;

/**
 * A vontade de fugir venceu.
 *
 * Só com Flee, ele corre em linha reta para longe, bate na parede, o Obstacle Avoidance
 * empurra de volta e ele fica quicando. Por isso a fuga combina duas forças:
 *  - Seek até um PONTO DE FUGA: chão aberto aonde o corpo dele passa em linha reta
 *    e onde ele chega antes do zumbi (dá o rumo);
 *  - Flee de cada zumbi que vê, com peso menor (mantém a vontade de se afastar).
 *
 * Sem ponto de fuga, está encurralado. Longe o bastante, volta a procurar itens.
 */
public class EstadoFugindo extends AbstractState<Sobrevivente> {

    private static final double PESO_SEEK_DA_ROTA = 1.0;
    private static final double PESO_FLEE = 0.5;
    private static final double SEGUNDOS_POR_ROTA = 1.5; // de tempos em tempos reavalia a rota
    private static final double DISTANCIA_PARA_CHEGAR = 25;

    private Vector2 pontoDeFuga = null;
    private double segundosNaRota = 0;

    public EstadoFugindo(Sobrevivente sobrevivente) {
        super(sobrevivente);
    }

    @Override
    public String getNome() {
        return "Fugindo";
    }

    @Override
    public void execute(World world) {
        Sobrevivente sobrevivente = getCharacter();

        // Enquanto foge, ele fica de olho para trás até a distância segura (maior que a visão normal),
        // senão pararia de fugir assim que o zumbi saísse do círculo de visão.
        double distanciaSegura = sobrevivente.getPersonalidade().getDistanciaSegura();
        List<Zumbi> ameacas = sobrevivente.zumbisVisiveis(distanciaSegura);

        // Transição: fugiu o suficiente
        if (ameacas.isEmpty()) {
            sobrevivente.setState(new EstadoProcurando(sobrevivente));
            return;
        }

        segundosNaRota += world.getSecs();
        if (precisaDeNovaRota(sobrevivente)) {
            pontoDeFuga = sobrevivente.escolherPontoDeFuga(ameacas);
            segundosNaRota = 0;
        }

        // Transição: não existe saída
        if (pontoDeFuga == null) {
            sobrevivente.setState(new EstadoEncurralado(sobrevivente));
            return;
        }

        // Forças
        sobrevivente.aplicarForca(Comportamento.SEEK, sobrevivente.seek(pontoDeFuga), PESO_SEEK_DA_ROTA);
        for (Zumbi zumbi : ameacas) {
            sobrevivente.aplicarForca(Comportamento.FLEE, sobrevivente.flee(zumbi.getPosition()), PESO_FLEE);
        }
    }

    private boolean precisaDeNovaRota(Sobrevivente sobrevivente) {
        if (pontoDeFuga == null) return true;
        boolean chegou = sobrevivente.distanciaAte(pontoDeFuga) < DISTANCIA_PARA_CHEGAR;
        boolean perdeuDeVista = !sobrevivente.temLinhaDeVisaoAte(pontoDeFuga);
        return chegou || perdeuDeVista || segundosNaRota > SEGUNDOS_POR_ROTA;
    }

    /** Para o debug desenhar o destino da fuga. */
    public Vector2 getPontoDeFuga() {
        return pontoDeFuga == null ? null : pontoDeFuga.clone();
    }
}

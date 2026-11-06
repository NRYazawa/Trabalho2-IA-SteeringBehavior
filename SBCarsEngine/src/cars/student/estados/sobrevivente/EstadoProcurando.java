package cars.student.estados.sobrevivente;

import cars.engine.Vector2;
import cars.engine.World;
import cars.student.Item;
import cars.student.agentes.Agente;
import cars.student.agentes.Comportamento;
import cars.student.agentes.Sobrevivente;
import cars.student.fsm.AbstractState;

import java.util.ArrayList;
import java.util.List;

/**
 * Explora o escritório até ver um item ou um zumbi.
 *
 * Só com Wander ele anda sem rumo e fica batendo nas paredes. Por isso escolhe um
 * DESTINO (um ponto de chão aberto aonde consegue ir em linha reta, de preferência à frente, para não
 * ficar dando meia-volta) e vai até ele com Seek. O Wander com peso menor deixa o
 * caminho com curvas naturais.
 */
public class EstadoProcurando extends AbstractState<Sobrevivente> {

    private static final double PESO_SEEK = 1.0;
    private static final double PESO_WANDER = 0.5;
    private static final double DISTANCIA_MINIMA_DO_DESTINO = 120;
    private static final double DISTANCIA_MAXIMA_DO_DESTINO = 300;
    private static final double DISTANCIA_PARA_CHEGAR = 25;
    private static final double SEGUNDOS_MAXIMOS_POR_DESTINO = 6;

    private Vector2 destino = null;
    private double segundosNoDestino = 0;

    public EstadoProcurando(Sobrevivente sobrevivente) {
        super(sobrevivente);
    }

    @Override
    public String getNome() {
        return "Procurando itens";
    }

    @Override
    public void execute(World world) {
        Sobrevivente sobrevivente = getCharacter();

        segundosNoDestino += world.getSecs();
        if (precisaDeNovoDestino(sobrevivente)) {
            destino = escolherDestino(sobrevivente);
            segundosNoDestino = 0;
        }

        // Forças
        if (destino != null) {
            sobrevivente.aplicarForca(Comportamento.SEEK, sobrevivente.seek(destino), PESO_SEEK);
        }
        sobrevivente.aplicarForca(Comportamento.WANDER, sobrevivente.wander(world.getSecs()), PESO_WANDER);

        // Transições
        switch (sobrevivente.decidirIntencao()) {
            case LUTAR -> sobrevivente.setState(new EstadoAtirando(sobrevivente));
            case FUGIR -> sobrevivente.setState(new EstadoFugindo(sobrevivente));
            case LOOTEAR -> {
                Item item = sobrevivente.getItemVisto();
                if (item != null) sobrevivente.setState(new EstadoIndoAoItem(sobrevivente, item));
            }
        }
    }

    private boolean precisaDeNovoDestino(Sobrevivente sobrevivente) {
        if (destino == null) return true;
        boolean chegou = sobrevivente.distanciaAte(destino) < DISTANCIA_PARA_CHEGAR;
        boolean perdeuDeVista = !sobrevivente.temLinhaDeVisaoAte(destino);
        return chegou || perdeuDeVista || segundosNoDestino > SEGUNDOS_MAXIMOS_POR_DESTINO;
    }

    /** Sorteia um ponto alcançável; se houver algum à frente, sorteia só entre os da frente. */
    private Vector2 escolherDestino(Sobrevivente sobrevivente) {
        List<Vector2> alcancaveis = sobrevivente.getSimulacao().getMapa()
                .pontosAlcancaveis(sobrevivente.getPosition(), DISTANCIA_MINIMA_DO_DESTINO, DISTANCIA_MAXIMA_DO_DESTINO, Agente.RAIO);

        List<Vector2> aFrente = new ArrayList<>();
        for (Vector2 ponto : alcancaveis) {
            Vector2 paraOPonto = Vector2.normalize(Vector2.subtract(ponto, sobrevivente.getPosition()));
            if (paraOPonto.dot(sobrevivente.getDirection()) > 0.3) aFrente.add(ponto);
        }

        List<Vector2> opcoes = aFrente.isEmpty() ? alcancaveis : aFrente;
        if (opcoes.isEmpty()) return null; // nada à vista: só o Wander
        return opcoes.get((int) (Math.random() * opcoes.size()));
    }

    /** Para o debug desenhar o destino da exploração. */
    public Vector2 getDestino() {
        return destino == null ? null : destino.clone();
    }
}

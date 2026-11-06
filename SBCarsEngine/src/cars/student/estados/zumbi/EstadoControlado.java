package cars.student.estados.zumbi;

import cars.engine.Vector2;
import cars.engine.World;
import cars.student.agentes.Comportamento;
import cars.student.agentes.Zumbi;
import cars.student.fsm.AbstractState;

import static cars.engine.Vector2.multiply;
import static cars.engine.Vector2.subtract;

/**
 * Zumbi do jogador (Modo Teste). As setas dizem a direção desejada;
 * um clique no mapa manda o zumbi até lá com Arrive. Sem comando, ele freia.
 */
public class EstadoControlado extends AbstractState<Zumbi> {

    private static final double RAIO_DE_FRENAGEM = 40;

    private Vector2 destinoDoClique = null;

    public EstadoControlado(Zumbi zumbi) {
        super(zumbi);
    }

    @Override
    public String getNome() {
        return "Controlado";
    }

    @Override
    public void execute(World world) {
        Zumbi zumbi = getCharacter();
        if (world.getClickPos() != null) destinoDoClique = world.getClickPos(); // a engine entrega cada clique uma vez
        Vector2 direcaoDasSetas = zumbi.getSimulacao().getTeclado().direcao();

        // Forças
        if (!direcaoDasSetas.isZero()) {
            destinoDoClique = null; // as setas têm prioridade sobre o clique
            Vector2 desejada = multiply(direcaoDasSetas, zumbi.getMaxSpeed());
            zumbi.aplicarForca(Comportamento.CONTROLE, subtract(desejada, zumbi.getVelocity()), 1.0);
        } else if (destinoDoClique != null) {
            zumbi.aplicarForca(Comportamento.ARRIVE, zumbi.arrive(destinoDoClique, RAIO_DE_FRENAGEM), 1.0);
        } else {
            // Arrive na própria posição = frear até parar
            zumbi.aplicarForca(Comportamento.ARRIVE, zumbi.arrive(zumbi.getPosition(), RAIO_DE_FRENAGEM), 1.0);
        }

        // Transição: o jogador ligou o modo automático ou trocou para o Modo Sobrevivente
        if (!zumbi.deveSerControlado()) {
            zumbi.setState(new EstadoPatrulhando(zumbi));
        }
    }
}

package cars.student.estados.sobrevivente;

import cars.engine.Vector2;
import cars.engine.World;
import cars.student.Item;
import cars.student.agentes.Comportamento;
import cars.student.agentes.Sobrevivente;
import cars.student.agentes.Zumbi;
import cars.student.fsm.AbstractState;

import static cars.engine.Vector2.*;

/**
 * Viu um zumbi: fica firme atirando. Se o zumbi passar do limite (a "distância mínima"
 * da personalidade), recua para manter esse limite.
 *
 * Um único Arrive faz as duas coisas: o alvo é um ponto na linha zumbi→sobrevivente,
 * a max(distância atual, distância mínima) do zumbi. Longe do limite, esse ponto é a
 * própria posição (ele para); dentro do limite, o ponto fica atrás dele (ele recua).
 * Assim nunca fazemos Seek e Flee no mesmo alvo.
 *
 * O Louco tem limite pequeno: deixa o zumbi chegar bem perto. O Medroso e o Cleptomaníaco
 * normalmente trocam para Fugindo antes de precisar recuar (a vontade de fugir vence antes).
 */
public class EstadoAtirando extends AbstractState<Sobrevivente> {

    private static final double RAIO_DE_FRENAGEM = 40;

    public EstadoAtirando(Sobrevivente sobrevivente) {
        super(sobrevivente);
    }

    @Override
    public String getNome() {
        return "Atirando";
    }

    @Override
    public void execute(World world) {
        Sobrevivente sobrevivente = getCharacter();
        Zumbi alvo = sobrevivente.getZumbiVisto();

        if (alvo == null) { // matou ou perdeu de vista
            sobrevivente.setState(new EstadoProcurando(sobrevivente));
            return;
        }

        // Forças: parar onde está ou recuar até a distância mínima
        double distanciaMinima = sobrevivente.getPersonalidade().getDistanciaMinima();
        double distanciaDoPonto = Math.max(sobrevivente.distanciaAte(alvo.getPosition()), distanciaMinima);
        Vector2 doZumbiParaMim = normalize(subtract(sobrevivente.getPosition(), alvo.getPosition()));
        Vector2 pontoDeCombate = add(alvo.getPosition(), multiply(doZumbiParaMim, distanciaDoPonto));
        sobrevivente.aplicarForca(Comportamento.ARRIVE, sobrevivente.arrive(pontoDeCombate, RAIO_DE_FRENAGEM), 1.0);

        sobrevivente.atirarEm(alvo);

        // Transições
        switch (sobrevivente.decidirIntencao()) {
            case LUTAR -> { } // continua atirando
            case FUGIR -> sobrevivente.setState(new EstadoFugindo(sobrevivente));
            case LOOTEAR -> {
                Item item = sobrevivente.getItemVisto();
                if (item != null) sobrevivente.setState(new EstadoIndoAoItem(sobrevivente, item));
            }
        }
    }
}

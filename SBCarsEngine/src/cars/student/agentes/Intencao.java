package cars.student.agentes;

/** As três vontades que disputam a decisão do sobrevivente. */
public enum Intencao {
    LUTAR("Lutar"),
    FUGIR("Fugir"),
    LOOTEAR("Lootear");

    private final String nome;

    Intencao(String nome) {
        this.nome = nome;
    }

    public String getNome() {
        return nome;
    }
}

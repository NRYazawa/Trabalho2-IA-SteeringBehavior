package cars.student.agentes;

import cars.engine.Car;
import cars.engine.Vector2;
import cars.engine.World;
import cars.student.Caminho;
import cars.student.Opcoes;
import cars.student.Simulacao;
import cars.student.estados.zumbi.EstadoControlado;
import cars.student.estados.zumbi.EstadoPatrulhando;
import cars.student.fsm.Character;
import cars.student.fsm.State;

import java.awt.Color;
import java.util.List;

/**
 * O inimigo. Um deles pode ser controlado pelo jogador (setas + mouse) no Modo Teste;
 * os outros, e todos no Modo Sobrevivente, andam sozinhos:
 *   Patrulhando (Path Following) → viu o sobrevivente → Perseguindo (Seek/Arrive)
 *   → perdeu → Vagando (Wander) → achou a rota de novo → Patrulhando.
 */
public class Zumbi extends Agente implements Character<Zumbi> {

    public static final double VELOCIDADE_MAXIMA = 60;
    public static final double FORCA_MAXIMA = 140;
    public static final double VIDA_MAXIMA = 125;
    public static final double RAIO_DE_VISAO = 220;
    public static final double RAIO_DE_SEPARACAO = 45;
    /** Até que distância ele procura um pedaço visível da rota para começar a patrulhar. */
    public static final double ALCANCE_PARA_ACHAR_A_ROTA = 250;

    private static final Color COR = new Color(95, 140, 60);
    private static int proximoId = 1;

    private final int id = proximoId++;
    private final boolean doJogador;
    /** Cada zumbi faz a ronda num sentido (sorteado), para não andarem todos em fila. */
    private final Caminho rota;
    private volatile State<Zumbi> estado; // volatile: a janela de Debug lê o nome do estado

    public Zumbi(Simulacao simulacao, Vector2 posicao, boolean doJogador) {
        super(simulacao, posicao, COR, VELOCIDADE_MAXIMA, FORCA_MAXIMA, VIDA_MAXIMA);
        this.doJogador = doJogador;
        Caminho rotaDoMapa = simulacao.getMapa().getRotaDePatrulha();
        this.rota = Math.random() < 0.5 ? rotaDoMapa : rotaDoMapa.invertido();
        setState(deveSerControlado() ? new EstadoControlado(this) : new EstadoPatrulhando(this));
    }

    @Override
    protected void executarEstado(World world) {
        estado.execute(world);
    }

    /** Só o zumbi do jogador, no Modo Teste e com o modo automático desligado. */
    public boolean deveSerControlado() {
        Opcoes opcoes = simulacao.getOpcoes();
        return doJogador && opcoes.modo == Opcoes.ModoDeJogo.TESTE && !opcoes.modoAutomatico;
    }

    public boolean consegueVerOSobrevivente() {
        return consegueVer(simulacao.getSobrevivente().getPosition(), RAIO_DE_VISAO);
    }

    /** Existe algum pedaço da rota de patrulha à vista (e não muito longe)? */
    public boolean enxergaARota() {
        return rota.projecaoVisivelMaisProxima(getPosition(), simulacao.getMapa(), ALCANCE_PARA_ACHAR_A_ROTA) != null;
    }

    /** Separation usando a vizinhança que a própria engine entrega (World.getNeighbors). */
    public Vector2 separacaoDosOutrosZumbis(World world) {
        List<Car> zumbisPerto = world.getNeighbors((int) RAIO_DE_SEPARACAO).stream()
                .filter(carro -> carro instanceof Zumbi)
                .toList();
        return separation(zumbisPerto, RAIO_DE_SEPARACAO);
    }

    // =====================================================================
    //  MÁQUINA DE ESTADOS
    // =====================================================================

    @Override
    public void setState(State<Zumbi> novoEstado) {
        if (estado != null) estado.leave();
        estado = novoEstado;
        printStats("-> " + novoEstado.getNome());
        estado.enter();
    }

    @Override
    public State<Zumbi> getState() {
        return estado;
    }

    @Override
    public String getNomeDoEstado() {
        return estado.getNome();
    }

    @Override
    public void printStats(String mensagem) {
        System.out.printf("[Zumbi #%d%s] %s | vida %.0f%n", id, doJogador ? " (jogador)" : "", mensagem, getVida());
    }

    public Caminho getRota() {
        return rota;
    }

    public boolean isDoJogador() {
        return doJogador;
    }
}

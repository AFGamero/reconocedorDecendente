import java.util.*;
import javax.swing.JOptionPane;

/**
 * ============================================================
 *  RECONOCEDOR DESCENDENTE CON PILA (LL(1))
 *  Taller de Gramaticas y Reconocimiento Descendente
 * ============================================================
 *
 *  GRAMATICA usada (expresiones aritmeticas LL(1)):
 *
 *    E  → T E'
 *    E' → + T E' | - T E' | ε
 *    T  → F T'
 *    T' → * F T' | / F T' | ε
 *    F  → ( E ) | id | num
 *
 *  Esta gramatica es LL(1): sin recursion izquierda y factorizada.
 *  Reconoce el lenguaje de expresiones aritmeticas con +, -, *, /, parentesis.
 *
 *  TABLA DE TRANSICION (Parsing Table):
 *  Cada celda [No-Terminal][Terminal] indica que produccion aplicar.
 * ============================================================
 */
public class Main {

    // ─── SIMBOLOS DE LA GRAMATICA ───────────────────────────────────────────────

    // No-terminales
    static final String E = "E";
    static final String EP = "E'"; // E'  (E prima)
    static final String T = "T";
    static final String TP = "T'"; // T'  (T prima)
    static final String F = "F";

    // Terminales
    static final String PLUS = "+";
    static final String MINUS = "-";
    static final String TIMES = "*";
    static final String DIV = "/";
    static final String LPAREN = "(";
    static final String RPAREN = ")";
    static final String ID = "id";
    static final String NUM = "num";
    static final String EOF = "$"; // Marcador de fin de cadena
    static final String EPSILON = "ε"; // Produccion vacia

    // Simbolo inicial y fondo de pila
    static final String START = E;
    static final String BOTTOM = "$";

    // ─── TABLA LL(1) ────────────────────────────────────────────────────────────
    // parseTable[noTerminal][terminal] = List<String> con los simbolos de la produccion
    // null significa ERROR (casilla vacia en la tabla)

    static final Map<String, Map<String, List<String>>> parseTable =
        new HashMap<>();

    static {
        // E → T E'       para: id, num, (
        put(E, ID, Arrays.asList(T, EP));
        put(E, NUM, Arrays.asList(T, EP));
        put(E, LPAREN, Arrays.asList(T, EP));

        // E' → + T E'    para: +
        put(EP, PLUS, Arrays.asList(PLUS, T, EP));
        // E' → - T E'    para: -
        put(EP, MINUS, Arrays.asList(MINUS, T, EP));
        // E' → ε         para: ), $
        put(EP, RPAREN, Collections.emptyList()); // ε
        put(EP, EOF, Collections.emptyList()); // ε

        // T → F T'       para: id, num, (
        put(T, ID, Arrays.asList(F, TP));
        put(T, NUM, Arrays.asList(F, TP));
        put(T, LPAREN, Arrays.asList(F, TP));

        // T' → * F T'    para: *
        put(TP, TIMES, Arrays.asList(TIMES, F, TP));
        // T' → / F T'    para: /
        put(TP, DIV, Arrays.asList(DIV, F, TP));
        // T' → ε         para: +, -, ), $
        put(TP, PLUS, Collections.emptyList()); // ε
        put(TP, MINUS, Collections.emptyList()); // ε
        put(TP, RPAREN, Collections.emptyList()); // ε
        put(TP, EOF, Collections.emptyList()); // ε

        // F → ( E )      para: (
        put(F, LPAREN, Arrays.asList(LPAREN, E, RPAREN));
        // F → id         para: id
        put(F, ID, Collections.singletonList(ID));
        // F → num        para: num
        put(F, NUM, Collections.singletonList(NUM));
    }

    // Metodo auxiliar para cargar la tabla
    static void put(
        String nonTerminal,
        String terminal,
        List<String> production
    ) {
        parseTable
            .computeIfAbsent(nonTerminal, k -> new HashMap<>())
            .put(terminal, production);
    }

    // ─── CONJUNTO DE NO-TERMINALES ──────────────────────────────────────────────
    static final Set<String> NON_TERMINALS = new HashSet<>(
        Arrays.asList(E, EP, T, TP, F)
    );

    // ─── RECONOCEDOR ────────────────────────────────────────────────────────────

    /**
     * Reconoce si una lista de tokens pertenece al lenguaje.
     * Imprime cada paso de la tabla de reconocimiento.
     *
     * @param tokens  Lista de tokens de entrada (terminada en "$")
     * @return true si la cadena es aceptada, false si no.
     */
    static boolean reconocer(List<String> tokens) {
        // Inicializar pila: tope = simbolo inicial, fondo = $
        Deque<String> pila = new ArrayDeque<>();
        pila.push(BOTTOM); // primero el fondo
        pila.push(START); // encima el simbolo inicial

        int pos = 0; // posicion actual en la entrada

        System.out.println("\n" + "═".repeat(75));
        System.out.println(" RECONOCIMIENTO DESCENDENTE CON PILA");
        System.out.println("═".repeat(75));
        System.out.printf(
            "%-28s %-20s %-25s%n",
            "PILA (tope→)",
            "ENTRADA (pos→)",
            "ACCION"
        );
        System.out.println("─".repeat(75));

        while (true) {
            String tope = pila.peek(); // simbolos en tope de pila
            String token = tokens.get(pos); // token actual de entrada

            // Imprimir estado actual
            String pilaStr = pilaToString(pila);
            String entradaStr = entradaToString(tokens, pos);
            System.out.printf("%-28s %-20s ", pilaStr, entradaStr);

            // ── CASO 1: Tope y token son ambos $ → ACEPTAR
            if (tope.equals(BOTTOM) && token.equals(EOF)) {
                System.out.println("ACEPTAR");
                System.out.println("═".repeat(75));
                return true;
            }

            // ── CASO 2: Tope == token (terminal en tope) → HACER MATCH
            if (!NON_TERMINALS.contains(tope) && tope.equals(token)) {
                pila.pop();
                pos++;
                System.out.println("MATCH '" + token + "'");
                continue;
            }

            // ── CASO 3: Tope es no-terminal → consultar tabla
            if (NON_TERMINALS.contains(tope)) {
                Map<String, List<String>> row = parseTable.get(tope);
                if (row != null && row.containsKey(token)) {
                    List<String> produccion = row.get(token);
                    pila.pop(); // sacar el no-terminal

                    // Apilar la produccion al reves (para que el primero quede en tope)
                    List<String> reversed = new ArrayList<>(produccion);
                    Collections.reverse(reversed);
                    for (String sym : reversed) {
                        pila.push(sym);
                    }

                    // Mostrar produccion aplicada
                    String prodStr = produccion.isEmpty()
                        ? EPSILON
                        : String.join(" ", produccion);
                    System.out.println(tope + " → " + prodStr);
                    continue;
                } else {
                    // Casilla vacia en la tabla → ERROR
                    System.out.println(
                        "ERROR: no hay regla para [" + tope + ", " + token + "]"
                    );
                    System.out.println("═".repeat(75));
                    return false;
                }
            }

            // ── CASO 4: Terminal en pila ≠ token → ERROR de coincidencia
            System.out.println(
                "ERROR: se esperaba '" + tope + "' pero llego '" + token + "'"
            );
            System.out.println("═".repeat(75));
            return false;
        }
    }

    // ─── TOKENIZADOR SIMPLE ──────────────────────────────────────────────────────

    /**
     * Tokeniza una expresion aritmetica en texto.
     * Convierte numeros a "num", identificadores a "id", y operadores tal cual.
     * Agrega "$" al final automaticamente.
     *
     * Ejemplo: "a + 3 * (b - 1)"
     *   → ["id", "+", "num", "*", "(", "id", "-", "num", "$"]
     */
    static List<String> tokenizar(String expresion) {
        List<String> tokens = new ArrayList<>();
        int i = 0;
        expresion = expresion.trim();

        while (i < expresion.length()) {
            char c = expresion.charAt(i);

            // Saltar espacios
            if (Character.isWhitespace(c)) {
                i++;
                continue;
            }

            // Numero
            if (Character.isDigit(c)) {
                while (
                    i < expresion.length() &&
                    Character.isDigit(expresion.charAt(i))
                ) i++;
                tokens.add(NUM);
                continue;
            }

            // Identificador
            if (Character.isLetter(c) || c == '_') {
                while (
                    i < expresion.length() &&
                    (Character.isLetterOrDigit(expresion.charAt(i)) ||
                        expresion.charAt(i) == '_')
                ) {
                    i++;
                }
                tokens.add(ID);
                continue;
            }

            // Operadores y parentesis
            switch (c) {
                case '+':
                    tokens.add(PLUS);
                    break;
                case '-':
                    tokens.add(MINUS);
                    break;
                case '*':
                    tokens.add(TIMES);
                    break;
                case '/':
                    tokens.add(DIV);
                    break;
                case '(':
                    tokens.add(LPAREN);
                    break;
                case ')':
                    tokens.add(RPAREN);
                    break;
                default:
                    System.out.println(
                        "Caracter desconocido ignorado: '" + c + "'"
                    );
            }
            i++;
        }

        tokens.add(EOF); // agregar marcador de fin
        return tokens;
    }

    // ─── UTILIDADES DE VISUALIZACION ────────────────────────────────────────────

    static String pilaToString(Deque<String> pila) {
        List<String> lista = new ArrayList<>(pila); // tope al inicio
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < lista.size(); i++) {
            sb.append(lista.get(i));
            if (i < lista.size() - 1) sb.append(", ");
        }
        sb.append("]");
        String s = sb.toString();
        return s.length() > 27 ? s.substring(0, 24) + "...]" : s;
    }

    static String entradaToString(List<String> tokens, int pos) {
        StringBuilder sb = new StringBuilder();
        for (int i = pos; i < tokens.size(); i++) {
            sb.append(tokens.get(i));
            if (i < tokens.size() - 1) sb.append(" ");
        }
        String s = sb.toString();
        return s.length() > 19 ? s.substring(0, 16) + "..." : s;
    }

    // ─── PROGRAMA PRINCIPAL ──────────────────────────────────────────────────────

    public static void main(String[] args) {
        System.out.println(
            "╔══════════════════════════════════════════════════════════════════════════╗"
        );
        System.out.println(
            "║     RECONOCEDOR DESCENDENTE LL(1) — Gramatica de Expresiones            ║"
        );
        System.out.println(
            "╠══════════════════════════════════════════════════════════════════════════╣"
        );
        System.out.println(
            "║  Gramaticas:                                                              ║"
        );
        System.out.println(
            "║    E  → T E'                                                             ║"
        );
        System.out.println(
            "║    E' → + T E' | - T E' | ε                                             ║"
        );
        System.out.println(
            "║    T  → F T'                                                             ║"
        );
        System.out.println(
            "║    T' → * F T' | / F T' | ε                                             ║"
        );
        System.out.println(
            "║    F  → ( E ) | id | num                                                 ║"
        );
        System.out.println(
            "╚══════════════════════════════════════════════════════════════════════════╝\n"
        );

        String ultimoResultado = "";

        while (true) { //joptionpane
            String expr = JOptionPane.showInputDialog(
                null,
                ultimoResultado + "Ingrese una expresion aritmetica:",
                "Reconocedor LL(1)",
                JOptionPane.QUESTION_MESSAGE
            );

            if (expr == null) break;

            System.out.println("\n▶  Entrada: \"" + expr + "\"");
            List<String> tokens = tokenizar(expr);
            System.out.println("   Tokens : " + tokens);

            boolean resultado = reconocer(tokens);
            System.out.println(
                "\n   Resultado: " + (resultado ? "ACEPTADA" : "RECHAZADA")
            );
            System.out.println("─".repeat(75));

            ultimoResultado =
                "Expresion: \"" +
                expr +
                "\"\nResultado: " +
                (resultado ? " ACEPTADA" : " RECHAZADA") +
                "\n\n";
        }
    }
}

import java.util.*;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

/**
 * ============================================================
 *  ESCANER PARA LENGUAJE C SIMPLIFICADO
 *  Compiladores 2 - Tarea 2
 * ============================================================
 *
 *  Implementa un AFD (Automata Finito Determinista) para reconocer
 *  los siguientes tipos de tokens:
 *
 *    - Palabras Reservadas (Keywords): int, main, void, break, do,
 *        else, if, while, return, scanf, printf, auto, double,
 *        struct, case, enum, long, switch, char, extern, typedef,
 *        const, float, short, unsigned, continue, for, signed,
 *        default, goto, sizeof, volatile, static, register, union
 *    - Simbolos Especiales: {, }, [, ], (, ), ;, +, -, *, /, &,
 *        |, !, =, <, >, <<, >>, ==, !=, <=, >=, &&, ||, ,, ., ->,
 *        %, ^, ~
 *    - INT_NUM : uno o mas digitos
 *    - ID      : letra o guion bajo, seguido de letras, digitos o _
 *
 *  Directivas de preprocesamiento (#include, #define, etc.) son
 *  reconocidas y reportadas como PREPROCESSOR
 *  Cadenas de caracteres ("...") se reconocen como STRING_LITERAL
 *  Caracteres de escape dentro de cadenas son manejados
 *  Comentarios de linea (//) y bloque (/* ... * /) son ignorados
 * ============================================================
 */
public class Main {

    // ─── TABLA DE PALABRAS RESERVADAS ───────────────────────────────────────────
    // Mapea el lexema al nombre del token correspondiente
    static final Map<String, String> KEYWORDS = new HashMap<>();

    static {
        // Keywords del enunciado principal
        KEYWORDS.put("int", "INT");
        KEYWORDS.put("main", "MAIN");
        KEYWORDS.put("void", "VOID");
        KEYWORDS.put("break", "BREAK");
        KEYWORDS.put("do", "DO");
        KEYWORDS.put("else", "ELSE");
        KEYWORDS.put("if", "IF");
        KEYWORDS.put("while", "WHILE");
        KEYWORDS.put("return", "RETURN");
        KEYWORDS.put("scanf", "READ");
        KEYWORDS.put("printf", "WRITE");
        // Keywords adicionales del lenguaje C completo
        KEYWORDS.put("auto", "AUTO");
        KEYWORDS.put("double", "DOUBLE");
        KEYWORDS.put("struct", "STRUCT");
        KEYWORDS.put("case", "CASE");
        KEYWORDS.put("enum", "ENUM");
        KEYWORDS.put("long", "LONG");
        KEYWORDS.put("switch", "SWITCH");
        KEYWORDS.put("char", "CHAR");
        KEYWORDS.put("extern", "EXTERN");
        KEYWORDS.put("typedef", "TYPEDEF");
        KEYWORDS.put("const", "CONST");
        KEYWORDS.put("float", "FLOAT");
        KEYWORDS.put("short", "SHORT");
        KEYWORDS.put("unsigned", "UNSIGNED");
        KEYWORDS.put("continue", "CONTINUE");
        KEYWORDS.put("for", "FOR");
        KEYWORDS.put("signed", "SIGNED");
        KEYWORDS.put("default", "DEFAULT");
        KEYWORDS.put("goto", "GOTO");
        KEYWORDS.put("sizeof", "SIZEOF");
        KEYWORDS.put("volatile", "VOLATILE");
        KEYWORDS.put("static", "STATIC");
        KEYWORDS.put("register", "REGISTER");
        KEYWORDS.put("union", "UNION");
    }

    // ─── CLASE TOKEN ────────────────────────────────────────────────────────────
    // Representa un token con su tipo y valor (lexema)
    static class Token {

        String tipo;
        String valor;

        Token(String tipo, String valor) {
            this.tipo = tipo;
            this.valor = valor;
        }

        @Override
        public String toString() {
            return "Token: " + tipo + " \"" + valor + "\"";
        }
    }

    // ─── FUNCION DE ESCANEO (AFD) ────────────────────────────────────────────────
    /**
     * Recibe el codigo fuente C como cadena y retorna la lista de tokens
     * El AFD avanza caracter por caracter y decide el tipo de token segun
     * el estado actual y el caracter leido
     *
     * Estados implicitos del AFD:
     *   START        -> estado inicial, decide que token comenzar
     *   IN_ID        -> leyendo un identificador o keyword
     *   IN_NUM       -> leyendo un numero entero
     *   IN_STRING    -> leyendo una cadena entre comillas dobles
     *   IN_COMMENT   -> leyendo un comentario de linea o bloque
     *   IN_OP        -> leyendo un operador que puede ser de uno o dos chars
     *
     * @param codigo  Texto del programa C a escanear
     * @return        Lista de tokens reconocidos
     */
    static List<Token> escanear(String codigo) {
        List<Token> tokens = new ArrayList<>();
        int i = 0;
        int n = codigo.length();

        while (i < n) {
            char c = codigo.charAt(i);

            // ── Saltar espacios en blanco y saltos de linea
            if (Character.isWhitespace(c)) {
                i++;
                continue;
            }

            // ── Directivas de preprocesamiento (#include, #define, etc.)
            if (c == '#') {
                int inicio = i;
                // Leer hasta fin de linea
                while (i < n && codigo.charAt(i) != '\n') {
                    i++;
                }
                String directiva = codigo.substring(inicio, i).trim();
                tokens.add(new Token("PREPROCESSOR", directiva));
                continue;
            }

            // ── Comentarios de linea (//) y de bloque (/* ... */)
            if (c == '/' && i + 1 < n) {
                char sig = codigo.charAt(i + 1);
                if (sig == '/') {
                    // Comentario de linea: ignorar hasta fin de linea
                    while (i < n && codigo.charAt(i) != '\n') {
                        i++;
                    }
                    continue;
                }
                if (sig == '*') {
                    // Comentario de bloque: ignorar hasta encontrar */
                    i += 2;
                    while (i + 1 < n) {
                        if (
                            codigo.charAt(i) == '*' &&
                            codigo.charAt(i + 1) == '/'
                        ) {
                            i += 2;
                            break;
                        }
                        i++;
                    }
                    continue;
                }
            }

            // ── Cadenas de texto entre comillas dobles
            if (c == '"') {
                StringBuilder sb = new StringBuilder();
                sb.append('"');
                i++;
                while (i < n && codigo.charAt(i) != '"') {
                    // Manejar caracteres de escape: \n, \t, \r, \\, \0, \v, \f, \a, \", \'
                    if (codigo.charAt(i) == '\\' && i + 1 < n) {
                        sb.append(codigo.charAt(i));
                        sb.append(codigo.charAt(i + 1));
                        i += 2;
                    } else {
                        sb.append(codigo.charAt(i));
                        i++;
                    }
                }
                sb.append('"');
                i++; // consumir la comilla de cierre
                tokens.add(new Token("STRING_LITERAL", sb.toString()));
                continue;
            }

            // ── Cadenas entre comillas simples (char literal)
            if (c == '\'') {
                StringBuilder sb = new StringBuilder();
                sb.append('\'');
                i++;
                while (i < n && codigo.charAt(i) != '\'') {
                    if (codigo.charAt(i) == '\\' && i + 1 < n) {
                        sb.append(codigo.charAt(i));
                        sb.append(codigo.charAt(i + 1));
                        i += 2;
                    } else {
                        sb.append(codigo.charAt(i));
                        i++;
                    }
                }
                sb.append('\'');
                i++;
                tokens.add(new Token("CHAR_LITERAL", sb.toString()));
                continue;
            }

            // ── Identificadores y palabras reservadas
            // ID = (letra | _)(letra | digito | _)*
            if (Character.isLetter(c) || c == '_') {
                int inicio = i;
                while (
                    i < n &&
                    (Character.isLetterOrDigit(codigo.charAt(i)) ||
                        codigo.charAt(i) == '_')
                ) {
                    i++;
                }
                String lexema = codigo.substring(inicio, i);
                // Verificar si es keyword o identificador
                String tipo = KEYWORDS.getOrDefault(lexema, "ID");
                tokens.add(new Token(tipo, lexema));
                continue;
            }

            // ── Numeros enteros
            // INT_NUM = digito+
            if (Character.isDigit(c)) {
                int inicio = i;
                while (i < n && Character.isDigit(codigo.charAt(i))) {
                    i++;
                }
                tokens.add(new Token("INT_NUM", codigo.substring(inicio, i)));
                continue;
            }

            // ── Simbolos especiales y operadores (AFD para operadores de 1 o 2 chars)
            switch (c) {
                case '{':
                    tokens.add(new Token("LBRACE", "{"));
                    i++;
                    break;
                case '}':
                    tokens.add(new Token("RBRACE", "}"));
                    i++;
                    break;
                case '[':
                    tokens.add(new Token("LSQUARE", "["));
                    i++;
                    break;
                case ']':
                    tokens.add(new Token("RSQUARE", "]"));
                    i++;
                    break;
                case '(':
                    tokens.add(new Token("LPAR", "("));
                    i++;
                    break;
                case ')':
                    tokens.add(new Token("RPAR", ")"));
                    i++;
                    break;
                case ';':
                    tokens.add(new Token("SEMI", ";"));
                    i++;
                    break;
                case '+':
                    tokens.add(new Token("PLUS", "+"));
                    i++;
                    break;
                case '*':
                    tokens.add(new Token("MUL_OP", "*"));
                    i++;
                    break;
                case ',':
                    tokens.add(new Token("COMMA", ","));
                    i++;
                    break;
                case '.':
                    tokens.add(new Token("DOT", "."));
                    i++;
                    break;
                case '^':
                    tokens.add(new Token("XOR_OP", "^"));
                    i++;
                    break;
                case '~':
                    tokens.add(new Token("COMPL_OP", "~"));
                    i++;
                    break;
                case '%':
                    tokens.add(new Token("MOD_OP", "%"));
                    i++;
                    break;
                // '-' puede ser MINUS o ->
                case '-':
                    if (i + 1 < n && codigo.charAt(i + 1) == '>') {
                        tokens.add(new Token("ARROW", "->"));
                        i += 2;
                    } else {
                        tokens.add(new Token("MINUS", "-"));
                        i++;
                    }
                    break;
                // '/' ya fue manejado arriba (comentarios), aqui es DIV_OP
                case '/':
                    tokens.add(new Token("DIV_OP", "/"));
                    i++;
                    break;
                // '&' puede ser AND_OP o &&
                case '&':
                    if (i + 1 < n && codigo.charAt(i + 1) == '&') {
                        tokens.add(new Token("ANDAND", "&&"));
                        i += 2;
                    } else {
                        tokens.add(new Token("AND_OP", "&"));
                        i++;
                    }
                    break;
                // '|' puede ser OR_OP o ||
                case '|':
                    if (i + 1 < n && codigo.charAt(i + 1) == '|') {
                        tokens.add(new Token("OROR", "||"));
                        i += 2;
                    } else {
                        tokens.add(new Token("OR_OP", "|"));
                        i++;
                    }
                    break;
                // '!' puede ser NOT_OP o !=
                case '!':
                    if (i + 1 < n && codigo.charAt(i + 1) == '=') {
                        tokens.add(new Token("NOTEQ", "!="));
                        i += 2;
                    } else {
                        tokens.add(new Token("NOT_OP", "!"));
                        i++;
                    }
                    break;
                // '=' puede ser ASSIGN o ==
                case '=':
                    if (i + 1 < n && codigo.charAt(i + 1) == '=') {
                        tokens.add(new Token("EQ", "=="));
                        i += 2;
                    } else {
                        tokens.add(new Token("ASSIGN", "="));
                        i++;
                    }
                    break;
                // '<' puede ser LT, <= o <<
                case '<':
                    if (i + 1 < n && codigo.charAt(i + 1) == '<') {
                        tokens.add(new Token("SHL_OP", "<<"));
                        i += 2;
                    } else if (i + 1 < n && codigo.charAt(i + 1) == '=') {
                        tokens.add(new Token("LTEQ", "<="));
                        i += 2;
                    } else {
                        tokens.add(new Token("LT", "<"));
                        i++;
                    }
                    break;
                // '>' puede ser GT, >= o >>
                case '>':
                    if (i + 1 < n && codigo.charAt(i + 1) == '>') {
                        tokens.add(new Token("SHR_OP", ">>"));
                        i += 2;
                    } else if (i + 1 < n && codigo.charAt(i + 1) == '=') {
                        tokens.add(new Token("GTEQ", ">="));
                        i += 2;
                    } else {
                        tokens.add(new Token("GT", ">"));
                        i++;
                    }
                    break;
                default:
                    // Caracter no reconocido: reportar error y avanzar
                    tokens.add(new Token("ERROR", String.valueOf(c)));
                    i++;
                    break;
            }
        }

        return tokens;
    }

    // ─── PROGRAMA PRINCIPAL ──────────────────────────────────────────────────────

    public static void main(String[] args) {
        // Encabezado en consola
        System.out.println("=".repeat(70));
        System.out.println("  ESCANER PARA LENGUAJE C SIMPLIFICADO");
        System.out.println("  Compiladores 2 - Tarea 2");
        System.out.println("=".repeat(70));
        System.out.println("  Tipos de token reconocidos:");
        System.out.println(
            "    Keywords  : int, main, void, break, do, else, if,"
        );
        System.out.println(
            "                while, return, scanf, printf, y mas..."
        );
        System.out.println(
            "    Simbolos  : {, }, [, ], (, ), ;, +, -, *, /, &, |,"
        );
        System.out.println(
            "                !, =, <, >, <<, >>, ==, !=, <=, >=, &&,"
        );
        System.out.println("                ||, ,, ., ->, %, ^, ~");
        System.out.println("    INT_NUM   : secuencias de digitos");
        System.out.println("    ID        : identificadores");
        System.out.println("=".repeat(70));

        String ultimoResultado = "";

        while (true) {
            // Solicitar codigo C al usuario via JOptionPane
            String codigo = JOptionPane.showInputDialog(
                null,
                ultimoResultado + "Ingrese el codigo C a escanear:",
                "Escaner C - Compiladores 2",
                JOptionPane.QUESTION_MESSAGE
            );

            // Si el usuario cancela, terminar
            if (codigo == null) break;

            System.out.println("\nEntrada:\n" + codigo);
            System.out.println("-".repeat(70));

            // Ejecutar el escaner (AFD)
            List<Token> tokens = escanear(codigo);

            // Construir la salida
            StringBuilder sb = new StringBuilder();
            for (Token t : tokens) {
                System.out.println(t);
                sb.append(t).append("\n");
            }

            System.out.println("-".repeat(70));
            System.out.println("Total de tokens: " + tokens.size());

            // Mostrar resultado en un JScrollPane para manejar salidas largas
            JTextArea area = new JTextArea(sb.toString());
            area.setEditable(false);
            area.setFont(
                new java.awt.Font("Monospaced", java.awt.Font.PLAIN, 13)
            );
            JScrollPane scroll = new JScrollPane(area);
            scroll.setPreferredSize(new java.awt.Dimension(500, 400));

            ultimoResultado = "Tokens generados: " + tokens.size() + "\n\n";

            JOptionPane.showMessageDialog(
                null,
                scroll,
                "Tokens reconocidos (" + tokens.size() + ")",
                JOptionPane.INFORMATION_MESSAGE
            );
        }
    }
}

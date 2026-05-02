import java.util.*;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

/**
 * ESCANER PARA LENGUAJE C 
 * AFD que reconoce los siguientes tokens:
 *
 * Palabras Reservadas
 *   int, main, void, break, do, else, if, while, return, scanf, printf,
 *   auto, double, struct, case, enum, long, switch, char, extern, typedef,
 *   const, float, short, unsigned, continue, for, signed, default, goto,
 *   sizeof, volatile, static, register, union
 *
 * Directivas de preprocesamiento: #define, #include, #ifdef
 *
 * Simbolos Especiales
 *   { } [ ] ( ) ; + - * / & | ! = < > << >> == != <= >= && || ,
 *
 * Operadores adicionales
 *   ^ ~ % -> .
 *
 * Literales de cadena: "..." con caracteres de escape \n \0 \r \t \\ \v \f \a \" \'
 *
 * Comentarios // y comentarios de bloque ignorados
 *
 * INT_NUM : digito+
 * ID      : (letra | _)(letra | digito | _)*
 */
public class Main {

    // Mapeo lexema nombre de token para palabras reservadas
    static final Map<String, String> KEYWORDS = new HashMap<>();

    static {
        // Palabras reservadas
        KEYWORDS.put("int",      "INT");
        KEYWORDS.put("main",     "MAIN");
        KEYWORDS.put("void",     "VOID");
        KEYWORDS.put("break",    "BREAK");
        KEYWORDS.put("do",       "DO");
        KEYWORDS.put("else",     "ELSE");
        KEYWORDS.put("if",       "IF");
        KEYWORDS.put("while",    "WHILE");
        KEYWORDS.put("return",   "RETURN");
        KEYWORDS.put("scanf",    "READ");
        KEYWORDS.put("printf",   "WRITE");
        KEYWORDS.put("auto",     "AUTO");
        KEYWORDS.put("double",   "DOUBLE");
        KEYWORDS.put("struct",   "STRUCT");
        KEYWORDS.put("case",     "CASE");
        KEYWORDS.put("enum",     "ENUM");
        KEYWORDS.put("long",     "LONG");
        KEYWORDS.put("switch",   "SWITCH");
        KEYWORDS.put("char",     "CHAR");
        KEYWORDS.put("extern",   "EXTERN");
        KEYWORDS.put("typedef",  "TYPEDEF");
        KEYWORDS.put("const",    "CONST");
        KEYWORDS.put("float",    "FLOAT");
        KEYWORDS.put("short",    "SHORT");
        KEYWORDS.put("unsigned", "UNSIGNED");
        KEYWORDS.put("continue", "CONTINUE");
        KEYWORDS.put("for",      "FOR");
        KEYWORDS.put("signed",   "SIGNED");
        KEYWORDS.put("default",  "DEFAULT");
        KEYWORDS.put("goto",     "GOTO");
        KEYWORDS.put("sizeof",   "SIZEOF");
        KEYWORDS.put("volatile", "VOLATILE");
        KEYWORDS.put("static",   "STATIC");
        KEYWORDS.put("register", "REGISTER");
        KEYWORDS.put("union",    "UNION");
    }

    // nombre de token para directivas de preprocesamiento
    static final Map<String, String> DIRECTIVES = new HashMap<>();

    static {
        DIRECTIVES.put("define",  "PP_DEFINE");
        DIRECTIVES.put("elif",    "PP_ELIF");
        DIRECTIVES.put("else",    "PP_ELSE");
        DIRECTIVES.put("endif",   "PP_ENDIF");
        DIRECTIVES.put("error",   "PP_ERROR");
        DIRECTIVES.put("if",      "PP_IF");
        DIRECTIVES.put("ifdef",   "PP_IFDEF");
        DIRECTIVES.put("ifndef",  "PP_IFNDEF");
        DIRECTIVES.put("include", "PP_INCLUDE");
        DIRECTIVES.put("message", "PP_MESSAGE");
        DIRECTIVES.put("undef",   "PP_UNDEF");
    }

    // Representa un token con tipo y lexema
    static class Token {
        String tipo;
        String valor;

        Token(String tipo, String valor) {
            this.tipo  = tipo;
            this.valor = valor;
        }

        @Override
        public String toString() {
            return "Token: " + tipo + " \"" + valor + "\"";
        }
    }

    /**
     * Funcion de escaneo - AFD con los siguientes estados:
     *
     *   START, lee el siguiente caracter y decide a que estado ir
     *   IN_ID, acumula (letra | _)(letra | digito | _)* -> ID o keyword
     *   IN_NUM,acumula digito+ -> INT_NUM
     *   IN_OP, mira un caracter adelante para operadores de dos chars
     *   IN_STRING, acumula caracteres entre comillas dobles con escape
     *   IN_COMMENT, ignora hasta fin de linea (//) o hasta cierre 
     *   IN_PP , lee directiva de preprocesamiento (#...)
     *
     * @param codigo fuente C a escanear
     * @return lista de tokens reconocidos
     */
    static List<Token> escanear(String codigo) {
        List<Token> tokens = new ArrayList<>();
        int i = 0;
        int n = codigo.length();

        while (i < n) {
            char c = codigo.charAt(i);

            // START: ignorar espacios
            if (Character.isWhitespace(c)) {
                i++;
                continue;
            }

            // IN_PP: directiva de preprocesamiento
            if (c == '#') {
                i++; // consumir '#'
                // saltar espacios entre # y el nombre
                while (i < n && codigo.charAt(i) == ' ') i++;
                // leer nombre de la directiva
                int inicio = i;
                while (i < n && Character.isLetter(codigo.charAt(i))) i++;
                String nombre = codigo.substring(inicio, i);
                // leer el resto de la linea como valor
                int restoInicio = i;
                while (i < n && codigo.charAt(i) != '\n') i++;
                String resto = codigo.substring(restoInicio, i).trim();
                String tipo = DIRECTIVES.getOrDefault(nombre, "PP_UNKNOWN");
                String lexema = "#" + nombre + (resto.isEmpty() ? "" : " " + resto);
                tokens.add(new Token(tipo, lexema));
                continue;
            }

            // IN_COMMENT: comentario de linea //
            if (c == '/' && i + 1 < n && codigo.charAt(i + 1) == '/') {
                while (i < n && codigo.charAt(i) != '\n') i++;
                continue;
            }

            // IN_COMMENT: comentario de bloque /* ... */
            if (c == '/' && i + 1 < n && codigo.charAt(i + 1) == '*') {
                i += 2;
                while (i + 1 < n && !(codigo.charAt(i) == '*' && codigo.charAt(i + 1) == '/')) i++;
                i += 2; //  */
                continue;
            }

            // IN_STRING: cadena entre comillas dobles
            if (c == '"') {
                StringBuilder sb = new StringBuilder();
                sb.append('"');
                i++;
                while (i < n && codigo.charAt(i) != '"') {
                    // manejar secuencias de escape: \n \0 \r \t \\ \v \f \a \" \'
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
                i++; // comilla de cierre
                tokens.add(new Token("STRING_LITERAL", sb.toString()));
                continue;
            }

            // IN_ID: inicia con letra o _
            if (Character.isLetter(c) || c == '_') {
                int inicio = i;
                while (i < n && (Character.isLetterOrDigit(codigo.charAt(i)) || codigo.charAt(i) == '_')) {
                    i++;
                }
                String lexema = codigo.substring(inicio, i);
                String tipo = KEYWORDS.getOrDefault(lexema, "ID");
                tokens.add(new Token(tipo, lexema));
                continue;
            }

            // IN_NUM: inicia con digito
            if (Character.isDigit(c)) {
                int inicio = i;
                while (i < n && Character.isDigit(codigo.charAt(i))) {
                    i++;
                }
                tokens.add(new Token("INT_NUM", codigo.substring(inicio, i)));
                continue;
            }

            // IN_OP: simbolos de uno o dos caracteres
            switch (c) {
                case '{':
                    tokens.add(new Token("LBRACE",   "{"));  i++; break;
                case '}':
                    tokens.add(new Token("RBRACE",   "}"));  i++; break;
                case '[':
                    tokens.add(new Token("LSQUARE",  "["));  i++; break;
                case ']':
                    tokens.add(new Token("RSQUARE",  "]"));  i++; break;
                case '(':
                    tokens.add(new Token("LPAR",     "("));  i++; break;
                case ')':
                    tokens.add(new Token("RPAR",     ")"));  i++; break;
                case ';':
                    tokens.add(new Token("SEMI",     ";"));  i++; break;
                case '+':
                    tokens.add(new Token("PLUS",     "+"));  i++; break;
                case '*':
                    tokens.add(new Token("MUL_OP",   "*"));  i++; break;
                case ',':
                    tokens.add(new Token("COMMA",    ","));  i++; break;
                case '.':
                    tokens.add(new Token("DOT",      "."));  i++; break;
                case '%':
                    tokens.add(new Token("MOD_OP",   "%"));  i++; break;
                case '^':
                    tokens.add(new Token("XOR_OP",   "^"));  i++; break;
                case '~':
                    tokens.add(new Token("COMPL_OP", "~"));  i++; break;

                // '-'  MINUS o flecha ->
                case '-':
                    if (i + 1 < n && codigo.charAt(i + 1) == '>') {
                        tokens.add(new Token("ARROW",  "->")); i += 2;
                    } else {
                        tokens.add(new Token("MINUS",  "-"));  i++;
                    }
                    break;

                // '/'  DIV_OP 
                case '/':
                    tokens.add(new Token("DIV_OP",  "/")); i++; break;

                // '&'  AND_OP o ANDAND
                case '&':
                    if (i + 1 < n && codigo.charAt(i + 1) == '&') {
                        tokens.add(new Token("ANDAND", "&&")); i += 2;
                    } else {
                        tokens.add(new Token("AND_OP", "&"));  i++;
                    }
                    break;

                // '|'  OR_OP o OROR
                case '|':
                    if (i + 1 < n && codigo.charAt(i + 1) == '|') {
                        tokens.add(new Token("OROR",  "||")); i += 2;
                    } else {
                        tokens.add(new Token("OR_OP", "|"));  i++;
                    }
                    break;

                // '!'  NOT_OP o NOTEQ
                case '!':
                    if (i + 1 < n && codigo.charAt(i + 1) == '=') {
                        tokens.add(new Token("NOTEQ",  "!=")); i += 2;
                    } else {
                        tokens.add(new Token("NOT_OP", "!"));  i++;
                    }
                    break;

                // '='  ASSIGN o EQ
                case '=':
                    if (i + 1 < n && codigo.charAt(i + 1) == '=') {
                        tokens.add(new Token("EQ",     "==")); i += 2;
                    } else {
                        tokens.add(new Token("ASSIGN", "="));  i++;
                    }
                    break;

                // '<'  LT, LTEQ o SHL_OP
                case '<':
                    if (i + 1 < n && codigo.charAt(i + 1) == '<') {
                        tokens.add(new Token("SHL_OP", "<<")); i += 2;
                    } else if (i + 1 < n && codigo.charAt(i + 1) == '=') {
                        tokens.add(new Token("LTEQ",   "<=")); i += 2;
                    } else {
                        tokens.add(new Token("LT",     "<"));  i++;
                    }
                    break;

                // '>'  GT, GTEQ o SHR_OP
                case '>':
                    if (i + 1 < n && codigo.charAt(i + 1) == '>') {
                        tokens.add(new Token("SHR_OP", ">>")); i += 2;
                    } else if (i + 1 < n && codigo.charAt(i + 1) == '=') {
                        tokens.add(new Token("GTEQ",   ">=")); i += 2;
                    } else {
                        tokens.add(new Token("GT",     ">"));  i++;
                    }
                    break;

                default:
                    // Caracter fuera del lenguaje
                    tokens.add(new Token("ERROR", String.valueOf(c)));
                    i++;
                    break;
            }
        }

        return tokens;
    }

    public static void main(String[] args) {
        System.out.println("  ESCANER PARA LENGUAJE C");

        String ultimoResultado = "";

        while (true) {
            // JOptionPane
            String codigo = JOptionPane.showInputDialog(
                null,
                ultimoResultado + "Ingrese el codigo C a escanear:",
                "Escaner C - Compiladores 2",
                JOptionPane.QUESTION_MESSAGE
            );

            //  cierra el programa
            if (codigo == null) break;

            System.out.println("\nEntrada:\n" + codigo);
            System.out.println("-".repeat(60));

            // Ejecutar el AFD
            List<Token> tokens = escanear(codigo);

            // Imprimir en consola y acumular para la ventana
            StringBuilder sb = new StringBuilder();
            for (Token t : tokens) {
                System.out.println(t);
                sb.append(t).append("\n");
            }

            System.out.println("-".repeat(60));
            System.out.println("Total de tokens: " + tokens.size());

            // Mostrar resultado en ventana
            JTextArea area = new JTextArea(sb.toString());
            area.setEditable(false);
            area.setFont(new java.awt.Font("Monospaced", java.awt.Font.PLAIN, 13));
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
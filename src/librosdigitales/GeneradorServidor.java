package librosdigitales;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.*;
import java.security.*;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.*;

// Esta clase prepara un almacén V2 nuevo o migra una copia de los libros de la versión original.
// Nunca reemplaza el almacén anterior ni un archivo de configuración existente.
public final class GeneradorServidor {
    private GeneradorServidor() { }

    public static void main(String[] args) {
        try {
            String origen=null;
            String destino="servidor_v2";
            if(args.length>0 && args[0].equals("--migrar")) {
                if(args.length!=3) throw new IOException("Uso: GeneradorServidor --migrar carpetaAnterior carpetaNueva");
                origen=args[1]; destino=args[2];
            } else if(args.length==1) destino=args[0];
            else if(args.length>1) throw new IOException("Uso: GeneradorServidor [carpetaNueva]");
            generar(destino,"servidor-config-v2.txt",origen,5050);
            System.out.println("Preparación terminada. No se modificó config.txt ni el almacén anterior.");
            System.out.println("Servidor: ejecutar ServidorLibros con servidor-config-v2.txt.");
            System.out.println("Cliente: adaptar config-cliente-v2.ejemplo.txt y guardarlo como config.txt.");
        } catch(Exception e) { System.out.println("No se generaron o no se completaron los datos: "+e.getMessage()); }
    }

    // Se puede invocar con rutas temporales desde las pruebas automáticas.
    public static void generar(String rutaDatos,String rutaConfig,String origen,int puerto) throws IOException, GeneralSecurityException {
        Path datos=Path.of(rutaDatos).toAbsolutePath().normalize();
        Path config=Path.of(rutaConfig).toAbsolutePath().normalize();
        Path ejemplo=config.resolveSibling("config-cliente-v2.ejemplo.txt");
        if(config.startsWith(datos) || ejemplo.startsWith(datos)) throw new IOException("Las configuraciones deben estar fuera de la carpeta compartida.");
        if(Files.exists(datos) || Files.exists(config) || Files.exists(ejemplo))
            throw new IOException("El destino o la configuración ya existen. Elige rutas nuevas; no se sobrescriben datos.");
        if(puerto<1 || puerto>65535) throw new IOException("Puerto inválido.");
        ListaEnlazada<Libro> libros;
        String usuarios;
        String[] textos;
        if (origen == null) {
            libros = RepositorioServidor.analizarCatalogo(
                    "9780000000001|Don Quijote de la Mancha\n"
                    + "9780000000002|El principito\n"
                    + "9780000000003|Cien años de soledad\n"
                    + "9780000000004|Orgullo y prejuicio\n"
                    + "9780000000005|La metamorfosis\n"
                    + "9780000000006|Drácula\n"
                    + "9780000000007|Frankenstein\n"
                    + "9780000000008|La isla del tesoro\n"
                    + "9780000000009|Las aventuras de Sherlock Holmes\n"
                    + "9780000000010|Alicia en el país de las maravillas\n"
            );

            usuarios =
                    "gerber|" + HashUtil.sha256("gerber123") + "|true\n"
                    + "mogo|" + HashUtil.sha256("mogo123") + "|false\n";

            // Adaptaciones breves para probar lectura, cifrado y compresión.
            // No corresponden a capítulos originales de las obras.
            textos = new String[]{
                "DON QUIJOTE DE LA MANCHA\n"
                    + "Texto de demostración: adaptación breve.\n"
                    + "\nCapítulo de muestra: Una nueva aventura\n"
                    + "Alonso Quijano pasaba largas horas leyendo historias de caballeros.\n"
                    + "Con el tiempo, comenzó a imaginar que también podía recorrer los caminos\n"
                    + "para defender a quienes necesitaran ayuda.\n"
                    + "Preparó una vieja armadura y decidió llamarse don Quijote.\n"
                    + "Su caballo recibió el nombre de Rocinante.\n"
                    + "Más adelante, Sancho Panza lo acompañaría como escudero.\n"
                    + "Juntos descubrirían que una misma escena podía parecer muy distinta\n"
                    + "a los ojos de un hombre práctico y a los de un soñador.\n",

                "EL PRINCIPITO\n"
                    + "Texto de demostración: adaptación breve.\n"
                    + "\nCapítulo de muestra: El pequeño viajero\n"
                    + "Un aviador había quedado detenido en medio del desierto.\n"
                    + "Mientras intentaba reparar su avión, apareció un pequeño viajero.\n"
                    + "El niño hacía preguntas sencillas que resultaban difíciles de responder.\n"
                    + "Poco a poco, el aviador supo que venía de un planeta diminuto.\n"
                    + "Allí había dejado una rosa que ocupaba un lugar importante en su vida.\n"
                    + "Durante su viaje había conocido adultos preocupados por mandar,\n"
                    + "poseer cosas o recibir reconocimiento.\n"
                    + "Escuchándolo, el aviador empezó a mirar sus propias preocupaciones de otra manera.\n",

                "CIEN AÑOS DE SOLEDAD\n"
                    + "Texto de demostración: adaptación breve.\n"
                    + "\nCapítulo de muestra: La vida en Macondo\n"
                    + "Macondo era el escenario de la vida de la familia Buendía.\n"
                    + "En sus casas convivían las tareas cotidianas y los sucesos extraordinarios.\n"
                    + "José Arcadio Buendía sentía una curiosidad intensa por los descubrimientos.\n"
                    + "Úrsula dedicaba sus esfuerzos a sostener la casa y mantener unida a la familia.\n"
                    + "Con los años llegaron nuevas generaciones y cambiaron las costumbres del pueblo.\n"
                    + "Los amores, las disputas y los recuerdos dejaron huellas en cada habitación.\n"
                    + "Algunos nombres volvieron a repetirse, junto con decisiones y errores parecidos.\n"
                    + "La historia familiar quedó marcada por el paso del tiempo y la soledad.\n",

                "ORGULLO Y PREJUICIO\n"
                    + "Texto de demostración: adaptación breve.\n"
                    + "\nCapítulo de muestra: Primeras impresiones\n"
                    + "La llegada de nuevos vecinos despertó el interés de la familia Bennet.\n"
                    + "Una reunión ofreció la oportunidad de conocerlos y conversar con ellos.\n"
                    + "Elizabeth observaba a los invitados con atención y sentido del humor.\n"
                    + "El señor Darcy le pareció distante y demasiado orgulloso.\n"
                    + "Aquella impresión influyó en la manera en que interpretó sus acciones posteriores.\n"
                    + "Darcy también juzgó a las personas según sus propias ideas y expectativas.\n"
                    + "Ninguno comprendía todavía cuánto podía ocultarse detrás de una primera impresión.\n"
                    + "El tiempo les daría motivos para revisar sus opiniones.\n",

                "LA METAMORFOSIS\n"
                    + "Texto de demostración: adaptación breve.\n"
                    + "\nCapítulo de muestra: Una mañana diferente\n"
                    + "Gregor Samsa despertó y descubrió que su cuerpo había cambiado por completo.\n"
                    + "Intentó levantarse, pero sus movimientos ya no respondían como antes.\n"
                    + "A pesar de lo ocurrido, pensó en el trabajo y en las obligaciones de su familia.\n"
                    + "Desde el otro lado de la puerta comenzaron a llamarlo.\n"
                    + "Quiso explicar la situación, aunque su voz resultaba extraña.\n"
                    + "La habitación conocida se convirtió en un lugar difícil de recorrer.\n"
                    + "Su transformación alteraría la vida de todos los habitantes de la casa.\n"
                    + "La distancia entre Gregor y su familia empezó a crecer.\n",

                "DRÁCULA\n"
                    + "Texto de demostración: adaptación breve.\n"
                    + "\nCapítulo de muestra: El castillo\n"
                    + "Jonathan Harker viajó a una región montañosa para atender asuntos de trabajo.\n"
                    + "Su destino era el castillo del conde Drácula.\n"
                    + "El anfitrión lo recibió con cortesía y mostró interés por Inglaterra.\n"
                    + "Sin embargo, la tranquilidad inicial dio paso a una inquietud creciente.\n"
                    + "Jonathan encontraba puertas cerradas y no veía a otros habitantes del castillo.\n"
                    + "Los horarios y las costumbres del conde le parecían cada vez más extraños.\n"
                    + "Comenzó a registrar sus observaciones para intentar comprender lo que sucedía.\n"
                    + "Pronto sospechó que abandonar aquel lugar no sería tan sencillo como llegar.\n",

                "FRANKENSTEIN\n"
                    + "Texto de demostración: adaptación breve.\n"
                    + "\nCapítulo de muestra: El experimento\n"
                    + "Victor Frankenstein dedicó sus estudios a comprender los secretos de la vida.\n"
                    + "Su curiosidad se convirtió en una ambición que ocupaba todos sus pensamientos.\n"
                    + "Trabajó durante mucho tiempo, apartándose de sus amigos y de su familia.\n"
                    + "Finalmente logró dar vida al ser que había construido.\n"
                    + "Pero el resultado le provocó miedo y rechazó a su creación.\n"
                    + "La criatura quedó sola frente a un mundo que no comprendía.\n"
                    + "Su deseo de encontrar compañía chocaría con el rechazo de otras personas.\n"
                    + "Victor tendría que afrontar las consecuencias de haber creado una vida y abandonarla.\n",

                "LA ISLA DEL TESORO\n"
                    + "Texto de demostración: adaptación breve.\n"
                    + "\nCapítulo de muestra: El mapa\n"
                    + "Jim Hawkins vivía en una posada donde se alojó un marinero de pasado misterioso.\n"
                    + "La presencia del huésped trajo historias de navegación y visitantes inquietantes.\n"
                    + "Después de varios acontecimientos, Jim encontró un mapa entre sus pertenencias.\n"
                    + "El documento señalaba una isla y la ubicación de un tesoro.\n"
                    + "Pronto se organizó una expedición para buscarlo.\n"
                    + "Jim se embarcó entusiasmado, sin conocer las intenciones de toda la tripulación.\n"
                    + "Entre los marineros destacaba Long John Silver, un hombre hábil para ganarse la confianza.\n"
                    + "El viaje obligaría al muchacho a distinguir las apariencias de los verdaderos propósitos.\n",

                "LAS AVENTURAS DE SHERLOCK HOLMES\n"
                    + "Texto de demostración: escena original inspirada en los personajes.\n"
                    + "\nCapítulo de muestra: La visita inesperada\n"
                    + "Watson encontró a Holmes examinando una pequeña llave sobre su escritorio.\n"
                    + "Un visitante la había dejado esa mañana junto con una dirección incompleta.\n"
                    + "La llave tenía restos de pintura verde y una etiqueta de papel húmeda.\n"
                    + "Watson pensó que sería imposible averiguar a quién pertenecía.\n"
                    + "Holmes señaló que la pintura era reciente y que el papel conservaba parte de un nombre.\n"
                    + "Decidieron visitar la calle indicada antes de que cerraran los comercios.\n"
                    + "Allí encontraron una tienda con la puerta recién pintada del mismo color.\n"
                    + "La investigación apenas comenzaba, pero la llave ya había dejado de ser un objeto sin historia.\n",

                "ALICIA EN EL PAÍS DE LAS MARAVILLAS\n"
                    + "Texto de demostración: adaptación breve.\n"
                    + "\nCapítulo de muestra: Tras el conejo\n"
                    + "Alicia vio pasar a un conejo blanco que parecía tener mucha prisa.\n"
                    + "La curiosidad aumentó cuando observó que llevaba un reloj.\n"
                    + "Lo siguió hasta una madriguera y comenzó una caída sorprendentemente larga.\n"
                    + "Al llegar abajo, encontró un lugar lleno de puertas y objetos extraños.\n"
                    + "Pronto descubrió que su tamaño podía cambiar de forma inesperada.\n"
                    + "Las reglas que conocía no parecían servir para comprender aquel mundo.\n"
                    + "Cada encuentro traía nuevas preguntas y conversaciones desconcertantes.\n"
                    + "Alicia continuó explorando, decidida a descubrir adónde había llegado.\n"
        };
        } else {
            RepositorioServidor anterior=new RepositorioServidor(origen);
            anterior.cargarUsuarios();
            usuarios=ArchivoUtil.leerTexto(ArchivoUtil.unir(origen,"usuarios.txt"));
            libros=anterior.cargarCatalogo();
            textos=new String[libros.getTamano()];
            // Estos valores solo permiten importar el cifrado histórico que ya utilizaba el proyecto.
            for(int i=0;i<textos.length;i++) textos[i]=descifrarVersionAnterior(anterior.leerLibroCifrado(libros.obtener(i).getIsbn()));
        }
        for(String texto:textos) if(texto.length()>Huffman.MAX_CARACTERES) throw new IOException("Un libro excede el límite de caracteres.");
        byte[] claveAlmacen=HashUtil.aleatorios(32);
        Cifrador3DES cifrador=new Cifrador3DES(claveAlmacen);
        KeyPairGenerator generador=KeyPairGenerator.getInstance("RSA"); generador.initialize(2048);
        KeyPair identidad=generador.generateKeyPair();
        ArchivoUtil.escribirTexto(datos.resolve("usuarios.txt").toString(),usuarios);
        ArchivoUtil.escribirTexto(datos.resolve("catalogo.txt").toString(),RepositorioServidor.serializarCatalogo(libros));
        for(int i=0;i<textos.length;i++) ArchivoUtil.escribirTexto(datos.resolve("libros").resolve(libros.obtener(i).getIsbn()+".txt").toString(),cifrador.cifrar(textos[i]));
        String huella=HashUtil.hexadecimal(HashUtil.resumen(identidad.getPublic().getEncoded()));
        ArchivoUtil.escribirTexto(config.toString(),"# PRIVADO: mantener fuera de la carpeta compartida y no subir a Git\n"
                +"rutaDatos="+datos+"\ninterfaz=0.0.0.0\npuerto="+puerto+"\ntiempoEsperaMs=10000\n"
                +"claveAlmacen="+Protocolo.b64(claveAlmacen)+"\nidentidadPrivada="+Protocolo.b64(identidad.getPrivate().getEncoded())
                +"\nidentidadPublica="+Protocolo.b64(identidad.getPublic().getEncoded())+"\n");
        ArchivoUtil.escribirTexto(ejemplo.toString(),"# Adaptar hostServidor y rutaServidor en cada computadora cliente\n"
                +"rutaServidor="+datos+"\nrutaLocal=cliente_v2\nhostServidor=localhost\npuertoServidor="+puerto
                +"\ntiempoEsperaMs=5000\nhuellaServidor="+huella+"\n");
        System.out.println("Almacén V2 creado: "+datos);
        System.out.println("Configuración privada: "+config);
        System.out.println("Huella pública que se copia a los clientes: "+huella);
    }

    // Compatibilidad de importación exclusiva para los archivos Base64 del programa original.
    private static String descifrarVersionAnterior(String archivo) throws GeneralSecurityException, IOException {
        if(archivo.startsWith("TDES2|")) throw new IOException("El origen ya es V2; conserva su configuración privada en vez de migrarlo como V1.");
        BigInteger p=BigInteger.ONE.shiftLeft(521).subtract(BigInteger.ONE);
        BigInteger a=new BigInteger("918273645564738291827364556473829101");
        BigInteger b=new BigInteger("102938475610293847561029384756102938");
        BigInteger secreto=BigInteger.valueOf(5).modPow(a,p).modPow(b,p);
        byte[] resumen=HashUtil.resumen(secreto.toByteArray());
        Cipher motor=Cipher.getInstance("DESede/CBC/PKCS5Padding");
        motor.init(Cipher.DECRYPT_MODE,new SecretKeySpec(java.util.Arrays.copyOf(resumen,24),"DESede"),
                new IvParameterSpec(java.util.Arrays.copyOfRange(resumen,24,32)));
        try { return ArchivoUtil.decodificar(motor.doFinal(Base64.getDecoder().decode(archivo.trim()))); }
        catch(IllegalArgumentException e) { throw new IOException("Un libro anterior no contiene Base64 válido.",e); }
    }
}

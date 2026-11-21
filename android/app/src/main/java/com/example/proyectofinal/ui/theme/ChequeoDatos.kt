package com.example.proyectofinal.ui.theme

import androidx.annotation.DrawableRes
import com.example.proyectofinal.R

// Un punto a revisar dentro de una categoría, con su explicación (botón ⓘ)
data class PuntoChequeo(val titulo: String, val info: String)

// Una categoría del chequeo guiado (Motor, Frenos, ...)
data class CategoriaChequeo(
    val titulo: String,
    val descripcion: String,
    @DrawableRes val icono: Int,
    val puntos: List<PuntoChequeo>
)

// Todo el contenido del chequeo guiado está aquí: para agregar o cambiar puntos, edita esta lista
object ChequeoDatos {

    val categorias = listOf(
        CategoriaChequeo(
            "Motor", "Revisa los componentes principales", R.drawable.ic_chequeo_motor,
            listOf(
                PuntoChequeo(
                    "Revisar nivel de aceite",
                    "Con el motor frío y el auto en terreno plano, saca la varilla, límpiala, " +
                            "vuelve a meterla y sácala otra vez. El aceite debe quedar entre las marcas " +
                            "MIN y MAX. Si está bajo, rellena con el aceite que indica el manual."
                ),
                PuntoChequeo(
                    "Revisar posibles fugas",
                    "Mira el suelo donde estaciona el auto y bajo el capó. Manchas oscuras suelen " +
                            "ser aceite; verdes, rosadas o naranjas, refrigerante. Una fuga nueva " +
                            "conviene que la revise un mecánico."
                ),
                PuntoChequeo(
                    "Estado de correas",
                    "Con el motor apagado, busca grietas, hilachas o zonas brillantes en la " +
                            "correa. Chirridos al encender o acelerar pueden indicar que está floja o " +
                            "gastada. La correa de distribución se cambia por kilometraje, según el manual."
                ),
                PuntoChequeo(
                    "Revisar conectores eléctricos",
                    "Comprueba que los conectores y cables del motor estén firmes, sin cables " +
                            "pelados, humedad ni sulfato. Un conector suelto puede causar fallas al " +
                            "encender o que se prenda la luz de check engine."
                ),
                PuntoChequeo(
                    "Estado de bujías",
                    "Bujías gastadas provocan tirones, mayor consumo y dificultad para encender. " +
                            "Se cambian según el kilometraje que indica el manual. Si las sacas, el " +
                            "electrodo debe verse limpio y de color café claro."
                )
            )
        ),
        CategoriaChequeo(
            "Frenos", "Verifica el sistema de frenos", R.drawable.ic_chequeo_frenos,
            listOf(
                PuntoChequeo(
                    "Nivel del líquido de frenos",
                    "Mira el depósito (es transparente y tiene marcas MIN y MAX). El nivel debe " +
                            "estar entre ambas. Si baja seguido, puede haber una fuga o pastillas muy " +
                            "gastadas. El líquido también se cambia cada cierto tiempo (revisa tu manual)."
                ),
                PuntoChequeo(
                    "Desgaste de pastillas",
                    "Mirando por la llanta, la pastilla debería tener más de unos 3 mm de material. " +
                            "Un chillido agudo al frenar suele ser el indicador de desgaste avisando " +
                            "que hay que cambiarlas."
                ),
                PuntoChequeo(
                    "Estado de discos",
                    "Deben verse lisos y parejos. Surcos profundos, un borde con escalón o " +
                            "vibración en el volante o el pedal al frenar indican discos gastados o " +
                            "deformados."
                ),
                PuntoChequeo(
                    "Recorrido del pedal",
                    "El pedal debe sentirse firme. Si se hunde mucho, se siente esponjoso o queda " +
                            "muy abajo, puede haber aire o una fuga en el circuito. En ese caso es " +
                            "mejor no manejar y consultar."
                ),
                PuntoChequeo(
                    "Freno de mano",
                    "Debe sujetar el auto en una pendiente sin necesidad de levantarlo hasta el " +
                            "tope. Si tiene demasiado recorrido, necesita ajuste."
                )
            )
        ),
        CategoriaChequeo(
            "Neumáticos", "Estado y presión", R.drawable.ic_chequeo_neumaticos,
            listOf(
                PuntoChequeo(
                    "Presión de aire",
                    "Mídela con los neumáticos fríos, incluido el de repuesto. La presión " +
                            "recomendada está en una etiqueta en el marco de la puerta del conductor o " +
                            "en el manual (no la que aparece escrita en el neumático)."
                ),
                PuntoChequeo(
                    "Profundidad del dibujo",
                    "Busca los indicadores de desgaste dentro de las ranuras. El mínimo habitual " +
                            "es de 1,6 mm, pero conviene cambiarlos antes, porque con poco dibujo " +
                            "frenan peor, sobre todo en mojado."
                ),
                PuntoChequeo(
                    "Desgaste irregular",
                    "Si un lado o el centro se gasta más que el resto, puede haber problemas de " +
                            "alineación, balanceo o presión incorrecta."
                ),
                PuntoChequeo(
                    "Daños visibles",
                    "Busca cortes, grietas, objetos clavados o bultos en el costado. Un bulto en " +
                            "el costado es peligroso: hay que cambiar el neumático."
                ),
                PuntoChequeo(
                    "Rotación y alineación",
                    "Rotar los neumáticos y alinear la dirección ayuda a que se desgasten parejo. " +
                            "Suele hacerse cada 8.000 a 10.000 km, o antes si el auto se va hacia un lado."
                )
            )
        ),
        CategoriaChequeo(
            "Batería", "Voltaje y conexiones", R.drawable.ic_chequeo_bateria,
            listOf(
                PuntoChequeo(
                    "Voltaje",
                    "Con un multímetro y el motor apagado, una batería sana marca cerca de 12,6 V. " +
                            "Bajo 12,2 V está descargada. Con el motor encendido debería marcar entre " +
                            "13,7 y 14,7 V (es la carga del alternador)."
                ),
                PuntoChequeo(
                    "Bornes y conexiones",
                    "Los bornes deben estar firmes y limpios. Un polvo blanco o verdoso es " +
                            "sulfatación: se limpia con agua y bicarbonato, con la batería desconectada."
                ),
                PuntoChequeo(
                    "Fijación de la batería",
                    "Debe estar bien sujeta y sin moverse. Las vibraciones la dañan y acortan su vida."
                ),
                PuntoChequeo(
                    "Estado exterior",
                    "Revisa que la carcasa no esté hinchada, agrietada ni con fugas. Si es así, " +
                            "hay que cambiarla y manipularla con guantes."
                ),
                PuntoChequeo(
                    "Antigüedad y arranque",
                    "Una batería dura en promedio entre 3 y 5 años. Si el motor gira lento al " +
                            "encender o la batería tiene más de 4 años, conviene hacerle una prueba de carga."
                )
            )
        ),
        CategoriaChequeo(
            "Fluidos", "Aceite, refrigerante, frenos, etc.", R.drawable.ic_chequeo_fluidos,
            listOf(
                PuntoChequeo(
                    "Aceite del motor",
                    "Revisa el nivel entre MIN y MAX y el color. Dorado o café es normal; negro muy " +
                            "espeso o con aspecto lechoso necesita revisión."
                ),
                PuntoChequeo(
                    "Refrigerante",
                    "Con el motor frío, el nivel del depósito debe estar entre MIN y MAX. Nunca " +
                            "abras la tapa del radiador con el motor caliente."
                ),
                PuntoChequeo(
                    "Líquido de frenos",
                    "El depósito debe estar entre MIN y MAX. Si el líquido se ve muy oscuro, ya " +
                            "está viejo y conviene cambiarlo."
                ),
                PuntoChequeo(
                    "Líquido de dirección",
                    "Si tu auto tiene dirección hidráulica, revisa el nivel del depósito. Un " +
                            "quejido al girar el volante puede indicar nivel bajo. Muchos autos " +
                            "nuevos tienen dirección eléctrica y no lo usan."
                ),
                PuntoChequeo(
                    "Limpiaparabrisas",
                    "Rellena el depósito con líquido limpiaparabrisas. Evita usar solo agua: no " +
                            "limpia bien y, con heladas, puede congelarse."
                )
            )
        )
    )
}
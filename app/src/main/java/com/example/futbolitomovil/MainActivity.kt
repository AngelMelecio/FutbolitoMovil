package com.example.futbolitomovil

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Point
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.Display
import android.view.View
import android.view.WindowManager

//x
private var velocidadX = 0.0f
private var posX = 0.0f
private var xMax = 0.0f
private var acelerometroX = 0.0f

//y
private var velocidadY = 0.0f
private var posY = 0.0f
private var yMax = 0.0f
private var acelerometroY = 0.0f

private var aroInicio = 0.0f
private var aroFinal = 0.0f
private var frameTime = 0.666f

private val anchoPelota = 130
private val anchoAro = 265
private val alturaPelota = 130
private val alturaAro = 265

private var scoreA = 0
private var scoreB = 0
private var esCanasta = false
lateinit var pelota : Bitmap
lateinit var aroA : Bitmap
lateinit var aroB : Bitmap
lateinit var cancha : Bitmap

private val gravity = 9.81f // valor de la gravedad en m/s^2
private val friction = 0.95f // factor de fricción


class MainActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private  var sensorACCELEROMETER: Sensor? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        var tamanio = Point()
        var display : Display = windowManager.defaultDisplay
        display.getSize(tamanio)

        val miVista = MiVista(this)
        setContentView(miVista)

        xMax = tamanio.x.toFloat() - anchoPelota
        yMax = tamanio.y.toFloat() - alturaAro

        posX = ((xMax + anchoPelota) / 2) - (anchoPelota / 2)
        posY = ((yMax + anchoPelota) / 2) - (anchoPelota / 2)

        aroInicio = ((xMax + anchoPelota) / 2) - (anchoAro / 2)
        aroFinal = aroInicio + anchoAro

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        sensorACCELEROMETER = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    }

    override fun onSensorChanged(p0: SensorEvent?) {
        //TODO("Not yet implemented")
        if (p0?.sensor!!.getType() == Sensor.TYPE_ACCELEROMETER && !esCanasta) {
            acelerometroX = p0?.values!!.get(0)
            acelerometroY = -p0?.values!!.get(1)
            actualizarPosicion()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        //TODO("Not yet implemented")
    }

    override fun onResume() {
        super.onResume()
        if (sensorACCELEROMETER != null) {
            sensorManager.registerListener(
                this,
                sensorACCELEROMETER,
                SensorManager.SENSOR_DELAY_GAME
            )
        }
    }

    override fun onPause() {
        super.onPause()
        if (sensorACCELEROMETER != null) {
            sensorManager.unregisterListener(this)
        }
    }

    private fun actualizarPosicion() {
        //Calcular nueva posicion
        velocidadX += (acelerometroX * frameTime)
        velocidadY += (acelerometroY * frameTime)

        velocidadX *= friction
        velocidadY *= friction

        val xS = (velocidadX / 2 - frameTime)
        val yS = (velocidadY / 2 - frameTime)

        posX -= xS
        posY -= yS

        //posX += velocidadX * frameTime
        //posY += velocidadY * frameTime

        var anoto = false

        val lPort = 50

        //Verificar limites de la posicion en x
        if (posX > xMax) {
            posX = xMax
        } else if (posX < 0f) {
            posX = 0f
        } else if (posX > aroInicio - anchoPelota && posX < aroInicio
            && (posY > yMax - alturaAro || posY < alturaAro)) {
            posX = aroInicio - anchoPelota
        } else if (posX < aroFinal && posX > aroFinal - lPort && (posY > yMax - alturaAro || posY < alturaAro)) {
            posX = aroFinal

        } else if (!esCanasta && posX > aroInicio && posX < aroFinal && (posY > yMax + alturaPelota - alturaAro ||
                    posY < alturaAro - alturaPelota)) {
            esCanasta = true
            if (posY < alturaAro - alturaPelota) {
                scoreA++
                anoto = true
            } else if (posY > yMax + alturaPelota - alturaAro) {
                scoreB++
                anoto = false
            }
            //Mostrar alerta con indicadores
            val builder = AlertDialog.Builder(this)
            if (anoto) {
                builder.setTitle("Booom Chakalaka!")
                    .setMessage("Ha encestado Miami\nMiami: $scoreA\n\n Warriors: $scoreB")
            } else {
                builder.setTitle("Booom Chakalaka!")
                    .setMessage("Ha encestado Warriors\nMiami: $scoreA\n\n Warriors: $scoreB")
            }
            builder.setPositiveButton("Seguir Jugando") { dialog: DialogInterface, id: Int ->
                dialog.dismiss()
                posX = (xMax + anchoPelota) / 2 - anchoPelota / 2
                posY = (yMax + anchoPelota) / 2 - anchoPelota / 2
                esCanasta = false
            }

            //reinicio de valores y posiciones iniciales
            builder.setNegativeButton("Reiniciar Juego") { dialog: DialogInterface, id: Int ->
                scoreA = 0
                scoreB = 0
                posX = (xMax + anchoPelota) / 2 - anchoPelota / 2
                posY = (yMax + anchoPelota) / 2 - anchoPelota / 2
                esCanasta = false
                dialog.dismiss()
            }
            val dialog = builder.create()
            dialog.show()
        }

        //Verificar limites de la posicion en y
        if (posY > yMax) {
            posY = yMax
        } else if (posY < 0) {
            posY = 0f
        }
    }
    private class MiVista(context: Context?) : View(context) {
        private val metrics: DisplayMetrics = DisplayMetrics()
        private val height: Int
        private val width: Int

        init {
            (context?.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay.getMetrics(metrics)
            height = metrics.heightPixels
            width = metrics.widthPixels

            //Creacion de la pelota
            val pelotaSrc = BitmapFactory.decodeResource(resources, R.drawable.balon)
            pelota = Bitmap.createScaledBitmap(pelotaSrc, anchoPelota, alturaPelota, true)

            //Creacion de los aros
            val aroSrcS = BitmapFactory.decodeResource(resources, R.drawable.ring1)
            aroA = Bitmap.createScaledBitmap(aroSrcS, anchoAro, alturaAro, true)
            val aroSrcI = BitmapFactory.decodeResource(resources, R.drawable.ring2)
            aroB = Bitmap.createScaledBitmap(aroSrcI, anchoAro, alturaAro, true)

            //Creacion del fondo
            val canchaSrc = BitmapFactory.decodeResource(resources, R.drawable.cancha)
            cancha = Bitmap.createScaledBitmap(canchaSrc, width, height - 50, true)
            Log.d("Tamaños", "$height $width")
        }

        override fun onDraw(canvas: Canvas) {
            canvas.drawBitmap(cancha, 0f, -50f, null)


            //Dibujar aros
            canvas.drawBitmap(aroA, aroInicio, -50f, null)
            canvas.drawBitmap(aroB, aroInicio, yMax - 30 + alturaPelota - 200, null)

            //Dibujar pelota
            canvas.drawBitmap(pelota, posX, posY, null)

            //Dibujar cancha
            canvas.drawBitmap(pelota, posX, posY, null)
            invalidate()
        }
    }

}
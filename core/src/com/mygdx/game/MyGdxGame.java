package com.mygdx.game;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.TimeUtils;

import java.util.Iterator;

public class MyGdxGame extends ApplicationAdapter {
	//---------Carga de recursos-----------------//
	//-----------------Texturas------------------//
	private Texture dropImage;
	private Texture bucketImage;

	//-----------Sfx y musica--------------------//
	private Sound dropSound;
	private Music rainMusic;

	//------------Camara (Scena ????)------------//
	private OrthographicCamera camera;

	//------------Dibujador de sprite------------//
	private SpriteBatch batch;

	//Componente para guardar la pocision y el tamaño que se
	//representara en la escena.
	//Funciona para describir tanto el cubo como las gotas de lluvia.
	//---------------REPRESENTACIONES-------------//
	private Rectangle bucket;
	private Array<Rectangle> raindrops;


	//Ultima gota añadida
	private long lastDropTime; //El tiempo se almacena en nanosegundos

	//Crea los elementos de la pantalla
	@Override
	public void create () {
		//Asignar las texturas a las variables
		dropImage = new Texture(Gdx.files.internal("drop.png"));
		bucketImage = new Texture(Gdx.files.internal("bucket.png"));

		//Asignar sfx y musica a las variables
		dropSound = Gdx.audio.newSound(Gdx.files.internal("drop.wav"));
		rainMusic = Gdx.audio.newMusic(Gdx.files.internal("rain.mp3"));

		//Inicio de la musica de fondo automaticamente
		rainMusic.setLooping(true);
		rainMusic.play();

		//Crear la camara
		camera = new OrthographicCamera();
		camera.setToOrtho(false, 800, 480);

		//Dibujador de texturas
		batch = new SpriteBatch();

		//Inicializacion de los componentes
		/*
		Investigar....
		 */
		bucket = new Rectangle();

		/*
		bucket.x = 800 / 2 - 64 / 2;
		Lo que esto hace es calular la pocicion central donde colocar bucket.
		800 / 2 da como resultado 400, si solo tuvieramos esta parte, bucket se dibujaria
		un poco a la deracha, ya que desde el 400 inicia a dibujarse.

		En la grafica cada division de 10 giones con exclamacion ---------! representa 100px de la pantalla
		Cuando llegue a los 400 (800 / 2) comenzara a dibujarse bucket **********
		Como se nota, se visualizaria un poco a la derecha, ya que se coloca desde el costado de bucket.
		0px|---------!---------!---------!---------!**********---------!---------!---------!|800px


		Tenemos que dividir las medidas del bucket entre dos 64 / 2 = 32 Esto pada poder posicionarlo en el centro:
		#####*****

		Del rango de 0 - 400px le restamos la division de bucket. Seria un resutado de 368 aprx:
		|---------!---------!---------!-----

		Con eso, el la colocacion quedaria asi:
		0px|---------!---------!---------!-----#####*****---------!---------!---------!|800px

		Ahora podemos visualizarlo en el centro.

		(POSICION SIN CALCULO)
		0px|---------!---------!---------!---------!**********---------!---------!---------!|800px

		(POCISION CON CALCULO)
										 368px||32px|
		0px|---------!---------!---------!-----#####*****---------!---------!---------!|800px


		 */

		bucket.x = 800 / 2 - 64 / 2;
		bucket.y = 20;
		bucket.width = 64;
		bucket.height = 64;

		//--GENERACION DE RAINDROPS ALEATORIOS--//
		raindrops = new Array<>();
		spawnRaindrop();
	}

	//Dibujado
	//Ciclo de vida.
	@Override
	public void render () {
		ScreenUtils.clear(0,0, 0.2f,1);
		batch.setProjectionMatrix(camera.combined);
		batch.begin();
		//System.out.println(bucket.x);
		batch.draw(bucketImage, bucket.x, bucket.y);

		//Dibujado de raindrop
		for(Rectangle raindrop: raindrops){
			batch.draw(dropImage, raindrop.x, raindrop.y);
		}
		batch.end();


		//MOVIMIENTO DEL CUBO POR MOUSE CLICK

		if(Gdx.input.isTouched()){ //Si el mause tochea la escena

			//Vector 3d.
			//Se utiliza un vector 3d ya que nuestra camara OrthographicCaera es
			//en realidad una camara 3D
			Vector3 touchPos = new Vector3();

			//Gdx.input.getX(), Gdx.input.getY() devuelven las cordenadas donde
			//el mouse hiso click.

			//Al vector se le asignan las cordenedas del punto de toque del mouse.
			touchPos.set(Gdx.input.getX(), Gdx.input.getY(), 0);

			//Transforma las cordenadas del toque del mouse a cordenadas que puedan
			//ser representadas en nuestro juego.
			camera.unproject(touchPos);
			bucket.x = touchPos.x - 64 / 2;
		}


		//MOVIMIENTO DEL CUBO POR TECLADO
		if(Gdx.input.isKeyPressed(Input.Keys.LEFT)){
			bucket.x -= 500 * Gdx.graphics.getDeltaTime();
		}
		if(Gdx.input.isKeyPressed(Input.Keys.RIGHT)){
			bucket.x += 500 * Gdx.graphics.getDeltaTime();
		}


		//-----LIMITACION DEL MOVIMIENTO-----//
		/*
		Cuando la cordenada x sea menor a 0, posicionara bucket en x = 0.
		 */
		if(bucket.x < 0) bucket.x = 0;
		/*
		Cuando la cordenada x sea mayor a 736
		Posicionara bucket en x = 736. Recordemos que inicia a debujar desde el costado izquierdo de bucket.
		*/
		if(bucket.x > 800 - 64) bucket.x = 800 - 64;


		//1000000000n = 1s
		if(TimeUtils.nanoTime() - lastDropTime > 1000000000){
			spawnRaindrop();
		}

		//Hace genera el recorrido de las gotas callendo.
		for(Iterator<Rectangle> iter = raindrops.iterator(); iter.hasNext(); ){
			Rectangle raindrop = iter.next();
			raindrop.y -= 500 * Gdx.graphics.getDeltaTime();
			if(raindrop.y + 64 < 0){
				iter.remove();
			}

			if(raindrop.overlaps(bucket)){
				dropSound.play();
				iter.remove();
			}
		}
	}

	//Eliminacion de objetos
	@Override
	public void dispose () {
		dropImage.dispose();
		bucketImage.dispose();
		dropSound.dispose();
		rainMusic.dispose();
		batch.dispose();
	}

	//Span de gotas
	/*
	Metodo para generar gotas de lluvias en una posicion x
	aleatoria. Ademas de capturar el tiempo de cada spawn de
	gota (raindrop)
	 */
	private void spawnRaindrop(){
		Rectangle raindrop = new Rectangle();
		raindrop.x = MathUtils.random(0, 800 - 64);
		raindrop.y = 480;
		raindrop.width = 64;
		raindrop.height = 64;
		raindrops.add(raindrop);
		lastDropTime = TimeUtils.nanoTime(); //Guarda el tiempo en nanosegundos
		System.out.println(lastDropTime);
	}
}

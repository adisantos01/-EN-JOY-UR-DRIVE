package com.example.robapp;

import androidx.appcompat.app.AppCompatActivity;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class MainActivity extends AppCompatActivity{
    private TextView angleTextView;
    private TextView powerTextView;
    private TextView directionTextView;
    private JoystickView joystick;
    private TextView status_battery;
    //private TextView status;
    int angulo;
    double seno, cosseno, rad;
    String front_back="F", right_left ="D", message_to_robot = "S";


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //angleTextView = (TextView) findViewById(R.id.angleTextView);
        //powerTextView = (TextView) findViewById(R.id.powerTextView);
        //directionTextView = (TextView) findViewById(R.id.directionTextView);
        joystick = (JoystickView) findViewById(R.id.joystickView);
        status_battery = (TextView) findViewById(R.id.status_bat);
        Thread myTread = new Thread(new robotToApp());
        myTread.start();
        //status = (TextView) findViewById(R.id.status);


        joystick.setOnJoystickMoveListener(new JoystickView.OnJoystickMoveListener() {

            @Override
            public void onValueChanged(int angle, int power, int direction) {
                // TODO Auto-generated method stub
                angleTextView.setText(" " + String.valueOf(angle) + "°");
                powerTextView.setText(" " + String.valueOf(power) + "%");

                angulo=(360-angle)+90;
                if(angulo>360){angulo=angulo-360;}
                rad = (Math.PI/180) * angulo;
                seno = Math.sin(rad);
                cosseno = Math.cos(rad);
                //corrige se o seno e o cosseno para ter em conta o power
                seno=(seno/100)*power;

                //determina a velocidade frente/tras
                if ((seno <= -0.8))
                {front_back = "A";} //Para trás velocidade max (-5)
                if ((seno <= -0.6) && (seno > -0.8))
                {front_back = "B";} //Para trás velocidade (-4)
                if ((seno <= -0.4) && (seno > -0.6))
                {front_back = "C";} //Para trás velocidade (-3)
                if ((seno <= -0.25) && (seno > -0.4))
                {front_back = "D";} //Para trás velocidade (-2)
                if ((seno <= -0.1) && (seno > -0.25))
                {front_back = "E";} //Para trás velocidade min (-1)
                if ((seno <= 0.1) && (seno > -0.1))
                {front_back = "F";} //velocidade 0 (parado)
                if ((seno <= 0.25) && (seno > 0.1))
                {front_back = "G";} //Para frente velocidade min (1)
                if ((seno <= 0.4) && (seno > 0.25))
                {front_back = "H";} //Para frente velocidade (2)
                if ((seno <= 0.6) && (seno > 0.4))
                {front_back = "I";} //Para frente velocidade (3)
                if ((seno <= 0.8) && (seno > 0.6))
                {front_back = "J";} //Para frente velocidade (4)
                if ((seno > 0.8) )
                {front_back = "K";} //Para frente velocidade max (5)

                //determina o grau de curvatura
                if ((cosseno <= -0.7))
                {right_left = "A";} //Para esquerda velocidade max (-3)
                if ((cosseno <= -0.4) && (cosseno > -0.7))
                {right_left = "B";} //Para esquerda velocidade (-2)
                if ((cosseno <= -0.1) && (cosseno > -0.4))
                {right_left = "C";} //Para esquerda velocidade (-1)
                if ((cosseno <= 0.1) && (cosseno > -0.1))
                {right_left = "D";} //(0) sem curvatura
                if ((cosseno <= 0.4) && (cosseno > 0.1))
                {right_left = "E";} //Para direita velocidade (1)
                if ((cosseno <= 0.7) && (cosseno > 0.4))
                {right_left = "F";} //Para direita velocidade (2)
                if ((cosseno > 0.7))
                {right_left = "G";} //Para direita velocidade max (3)

            }
        }, JoystickView.DEFAULT_LOOP_INTERVAL);
    }



    //enviar para o robot os comandos
    class goToServer extends AsyncTask<String,Void,Void>
    {
        Socket s;
        PrintWriter writer;

        @Override
        protected Void doInBackground(String... voids) {

            try {
                String message_to_robot = voids[0];
                s = new Socket("192.168.1.100",8011);
                writer = new PrintWriter(s.getOutputStream());
                writer.write(message_to_robot);
                writer.flush();
                writer.close();
                //status.setVisibility(View.VISIBLE);


            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return null;
        }
    }

    //receber constantemente do robot as strings
    class robotToApp implements Runnable
    {
        Socket s;
        ServerSocket ss;
        InputStreamReader isr;
        BufferedReader bf;
        Handler h = new Handler();
        String mensagem;
        String message;
        //variavel ch
        char ch = 'A';
        char ultimo_char_comunicacao_enviado = 'A';
        char char_bat;
        String string_bat;
        int conta = 0;
        //variáveis codigo ascii
        int ascii_ch;
        int soma_para_check;
        long ciclo;

        Boolean string_recebida_validada;
        @Override
        public void run() {
            try {
                ss=new ServerSocket(8010);
                h.post(new Runnable() {
                    @Override
                    public void run() {
                        status_battery.setText("80%");
                    }
                });
                ciclo = 0;
                while(true) {
                    ciclo = ciclo + 1;
                    //recebe string vinda do robot
                    s = ss.accept();
                    isr = new InputStreamReader(s.getInputStream());
                    bf = new BufferedReader(isr);
                    mensagem = "";
                    while ((message = bf.readLine()) != null) {
                        mensagem = mensagem + message;
                    }
                    if (mensagem.contains(String.valueOf("C"))) {
                        conta = 0;
                        while (mensagem.charAt(conta) != 'C') {
                            conta = conta + 1;
                        }

                        //falta validar a string verificando o checksum

                        //recolhe o byte que troca para avaliar a qualidade da comunicação
                        ch = mensagem.charAt(conta + 1);
                        //recolhe o byte do estado da bateria (3 byte)
                        char_bat = mensagem.charAt(conta + 2);
                        //publica o estado da bateria no ecran
                        if (char_bat == 'a') {
                            string_bat = "Bateria a 100%";
                        }
                        if (char_bat == 'b') {
                            string_bat = "Bateria a 80%";
                        }
                        if (char_bat == 'c') {
                            string_bat = "Bateria a 60%";
                        }
                        if (char_bat == 'd') {
                            string_bat = "Bateria a 40%";
                        }
                        if (char_bat == 'e') {
                            string_bat = "Bateria a 20%";
                        }
                        if (char_bat == 'f') {
                            string_bat = "Bateria a 0%";
                        }

                        h.post(new Runnable() {
                            @Override
                            public void run() {
                                status_battery.setText(mensagem + "   " + ciclo);
                            }
                        });
                    }

                    //compõe string para enviar


                    //concatenar string a enviar para o robot
                    message_to_robot = "S" + front_back + right_left + "AA";

                    //concatena o byte numero 6: troca 'A' e 'B' para validar a comunicação
                    if (ch == ultimo_char_comunicacao_enviado) {
                        if (ch == 'A') {
                            message_to_robot = message_to_robot + 'B';
                            ultimo_char_comunicacao_enviado = 'B';}
                        if (ch == 'B') {
                            message_to_robot = message_to_robot + 'A';
                            ultimo_char_comunicacao_enviado = 'A';} }
                    else{message_to_robot = message_to_robot + 'A';}
                    //O 7º char da string é um checkbyte que corresponde ao char ascii cuja ordem é:
                    //O resto da divisão inteira por 26 da soma dos valores dos bytes 2 a 6 da string
                    //(sendo A=1, B = 2, etc.) somado de 65 (para dar uma letra maiuscula):

                    ascii_ch = message_to_robot.charAt(1);
                    soma_para_check = ascii_ch - 64;
                    ascii_ch = message_to_robot.charAt(2);
                    soma_para_check = soma_para_check + ascii_ch - 64;
                    ascii_ch = message_to_robot.charAt(3);
                    soma_para_check = soma_para_check + ascii_ch - 64;
                    ascii_ch = message_to_robot.charAt(4);
                    soma_para_check = soma_para_check + ascii_ch - 64;
                    ascii_ch = message_to_robot.charAt(5);
                    soma_para_check = soma_para_check + ascii_ch - 64;
                    message_to_robot = message_to_robot + (char)((soma_para_check % 26) + 65);

                    //envia string atraves da instanciacao da classe
                    goToServer b1 = new goToServer();
                    b1.execute(message_to_robot);


                    h.post(new Runnable() {
                        @Override
                        public void run() {
                            status_battery.setText(message_to_robot);
                        }
                    });

                    //s.close();

                }
            } catch (IOException e) {
                throw new RuntimeException(e);}
        }
    }
}
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package main;

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Lucas Lobao and Carlos Damázio
 */
public class Battleship {
    
    static int[][] board = new int[7][7];

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws Exception {
        menu();        
    }
    
    private static void menu() throws Exception{
    	try{
            
            BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));
            System.out.println("Seja bem vindo ao BattleShip 1.0!");
            System.out.println("");
            System.out.println("Deseja se conectar a um segundo jogador ou esperar por um jogador?");
            System.out.println("Digite 1 para esperar ou 2 para conectar");
            String sentence = inFromUser.readLine();
            switch (sentence){
                case "1":
                    gameHoster();
                    
                case "2":
                    gameClient();
            }
        }catch(IOException e){
            System.out.println("Ocorreu um erro ao ler sua opção");
        }
    }
    
    private static void gameHoster() throws IOException{
        
        int port = 9876;
        try {
            DatagramSocket serverSocket = new DatagramSocket(port);        
        
            byte[] receiveData = new byte[1024];
            byte[] sendData = new byte[1024];

            while (true) {
                DatagramPacket receivePacket = new DatagramPacket(receiveData,receiveData.length);
                System.out.println("Esperando por segundo jogador na porta " + port);
                System.out.println("\n Por favor aguarde...");
                serverSocket.receive(receivePacket);
                InetAddress IPAddress = receivePacket.getAddress();
                                 port = receivePacket.getPort();
                clearConsole();                
                        
               receivePacket.getData();

                System.out.println("Segundo jogador conectado com sucesso!");
                
                
                System.out.println("Monte seu tabuleiro");
                //Configurando tabuleiro
                startBoard(board);

                setRandomSubmarino();
                setRandomSubmarino();
                setRandomSubmarino();
                setRandomCruzador();
                setRandomCruzador();
                setRandomPortaAvioes();
                
                showBoard(board);

                
                System.out.println("Aguarde enquanto o adversario monta o tabuleiro dele...");
                
                //Avisar o adversario de que o tabuleiro ja esta montado
                sendData = new String("").getBytes(); 
                DatagramPacket sendPacket = new DatagramPacket(sendData,sendData.length, IPAddress, port);
                serverSocket.send(sendPacket);
                waitYourTurn();
                
                //Aguardando o adversario montar o tabuleiro
                serverSocket.receive(receivePacket);
                
                //Inicio do jogo
                clearConsole();
                showBoard(board);
                BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));
                
                System.out.println("Tudo pronto, voce atira primeiro!");
                
                boolean running = true;
                
                while(running){
                    
                    //Preparar o tiro
                    int[] tiro = new int[2];

                    System.out.println("Digite a coordenada X do seu tiro: ");
                    int x = new Integer(inFromUser.readLine());

                    System.out.println("Digite a coordenada Y do seu tiro: ");
                    int y = new Integer(inFromUser.readLine());
                    tiro[0] = x;
                    tiro[1] = y;
                    
                    System.out.println("Voce atirou na posicao " + tiro[0] + "," + tiro [1] + " e...");

                    sendData = intsToBytes(tiro);
                    sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, port);
                    serverSocket.send(sendPacket);
                    
                    receivePacket = new DatagramPacket(receiveData,receiveData.length);
                    serverSocket.receive(receivePacket);
                    
                    String hit = new String(receivePacket.getData());
                    if(hit.contains("acertou")){
                        System.out.print("acertou o tiro!");
                    }else if(hit.contains("gameover")){
                    	System.out.print("Acertou o ultimo navio do adversario! Voce ganhou o jogo!");
                    	break;
                    }else{
                        System.out.print("Agua... :(");
                    }
                    System.out.println("");
                    
                    showBoard(board);
                    System.out.println("Aguarde o jogador 2 atirar...");
                    serverSocket.receive(receivePacket);
                    tiro = bytesToInts(receivePacket.getData());
                    clearConsole();
                    System.out.println("O jogador 2 atirou na posicao " + tiro[0] + "," + tiro [1] + " e...");
                    
                    //Verifica se tiro acertou navio
                    String doYouHit = shotOnBoard(tiro);
                    showBoard(board);
                    
                    
       
                    //Informar jogador se o tiro acertou
                    sendData = doYouHit.getBytes();
                    sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, port);
                    serverSocket.send(sendPacket);
                    
                    if(doYouHit.equals("gameover")){
                    	System.out.println("Ele... destruiu seu ultimo navio!");
                    	break;
                    }
                    
                    serverSocket.receive(receivePacket);
                    
                    sendPacket = null;
                    if(isGameOver()) running = false; 
                    
                }
                System.out.println("Fim de jogo!");
                new Integer(inFromUser.readLine());
                serverSocket.close();
            }
        
        } catch (SocketException ex) {
            Logger.getLogger(Battleship.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private static void gameClient() throws SocketException, IOException{
        
        clearConsole();
        
        //Objeto que sera usado para ler o teclado
	BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in)); 
 
        //Socket que sera utilizado para enviar pacote de dados para o servidor.
	DatagramSocket clientSocket = new DatagramSocket();
                
        System.out.println("Digite o nome do servidor: ");//"localhost"
	String servidor = inFromUser.readLine();
        
        //Pegar o endereço do servidor
	InetAddress hostIPAddress = InetAddress.getByName(servidor);
        
        System.out.println("Digite a porta do servidor (o padrao eh 9876): ");//"9876"
	int port = new Integer(inFromUser.readLine());
        
        //Inicializando variaveis que serao utilizadas para enviar e receber os dados do servidor
	byte[] sendData = new byte[1024];
	byte[] receiveData = new byte[1024];
        
        sendData = new String("conectei").getBytes();
        
        //Preparar pacote a ser enviado para o servidor
        DatagramPacket sendPacket = new DatagramPacket(sendData,sendData.length, hostIPAddress, port);        
        clientSocket.send(sendPacket);

                clearConsole();
                System.out.println("Conectado com sucesso!");
                
                System.out.println("Aguarde enquanto o adversario monta o tabuleiro...");
                DatagramPacket receivePacket = new DatagramPacket(receiveData,receiveData.length); 
		clientSocket.receive(receivePacket);
                /* Neste ponto o programa espera uma resposta do servidor antes de prosseguir com a execução*/
                
                System.out.println("Sua vez de montar o tabuleiro!");
                startBoard(board);
                
                setRandomSubmarino();
                setRandomSubmarino();
                setRandomSubmarino();
                setRandomCruzador();
                setRandomCruzador();
                setRandomPortaAvioes();
                
                showBoard(board);
                
                //Avisar o host que o tabuleiro esta pronto
                sendData = "tabuleiro montado".getBytes();
                sendPacket = new DatagramPacket(sendData, sendData.length, hostIPAddress, port);
                clientSocket.send(sendPacket);
                
                boolean running = true;
                //inicio do jogo
                clearConsole();
                showBoard(board);
                while(running){
                	
                    //Receber tiro
                   
                    System.out.println("Aguarde o jogador 1 atirar...");
                    receivePacket = new DatagramPacket(receiveData,receiveData.length);
                    clientSocket.receive(receivePacket);

                    int[] tiro = new int[2];
                    tiro = bytesToInts(receivePacket.getData());
                    clearConsole();
                    System.out.println("O jogador 1 atirou na posicao " + tiro[0] + "," + tiro [1] + " e...");
                    
                    //Verifica se tiro acertou navio
                    String doYouHit = shotOnBoard(tiro);
                    showBoard(board);
                    
       
                    //Informar jogador se o tiro acertou
                    sendData = doYouHit.getBytes();
                    sendPacket = new DatagramPacket(sendData, sendData.length, hostIPAddress, port);
                    clientSocket.send(sendPacket);
                    
                    if(doYouHit.equals("gameover")){
                    	System.out.println("Destruiu seu ultimo navio!");
                    	break;
                    }
                    
                    //Enviar tiro
                    System.out.println("Digite a coordenada X do seu tiro: ");
                    int x = new Integer(inFromUser.readLine());

                    System.out.println("Digite a coordenada Y do seu tiro: ");
                    int y = new Integer(inFromUser.readLine());
                    tiro[0] = x;
                    tiro[1] = y;
                    
                    System.out.println("Voce atirou na posicao " + tiro[0] + "," + tiro [1] + " e...");

                    sendData = intsToBytes(tiro);
                    sendPacket = new DatagramPacket(sendData, sendData.length, hostIPAddress, port);
                    clientSocket.send(sendPacket);
                    
                    //receber resposta do outro jogador
                    receivePacket = new DatagramPacket(receiveData,receiveData.length);
                    clientSocket.receive(receivePacket);
                    String hit = new String (receivePacket.getData());
                    
                    if(hit.contains("acertou")){
                        System.out.print("acertou o tiro!");
                    }else if(hit.contains("gameover")){
                    	System.out.print("Acertou o ultimo navio do adversario! Voce ganhou o jogo!");
                    	break;
                    }else{
	                        System.out.print("Agua... :(");
	                }
                    System.out.println("");
                    
                    showBoard(board);
                    clientSocket.send(sendPacket);
                    
                    if(isGameOver()) running = false;                    
                }
                System.out.println("Fim de jogo!");
                clientSocket.close();
    }
    
    private static void startBoard(int[][] board){
        for(int row=0 ; row < 7 ; row++ )
            for(int column=0 ; column < 7 ; column++ )
                board[row][column]=-1;
    }
    
    private static void showBoard(int[][] board){
        System.out.println("\t0 \t1 \t2 \t3 \t4 \t5 \t6");
        System.out.println();
        
        for(int row=0 ; row < 7 ; row++ ){
            System.out.print((row)+"");
            for(int column=0 ; column < 7 ; column++ ){
                if(board[row][column]==-1){
                    System.out.print("\t"+"~");
                }else if(board[row][column]==0){
                    System.out.print("\t"+"X");
                }else if(board[row][column]==1){
                    System.out.print("\t"+"S");
                }else if(board[row][column]==2){
                    System.out.print("\t"+"C");
                }else if(board[row][column]==3){
                    System.out.print("\t"+"P");
                }
                  
            }
            System.out.println();
        }
    }
    
    private static String shotOnBoard(int[] shot){
        if(board[shot[0]][shot[1]] == 1 || board[shot[0]][shot[1]] == 2 || board[shot[0]][shot[1]] == 3){
            System.out.print("Um de seus navios foi atingido!");
            board[shot[0]][shot[1]] = 0;
            System.out.println("");
            if(isGameOver()){
            	return "gameover";
            }
            return "acertou";
        }else{
            System.out.print("Este tiro nem passou perto!");
            System.out.println("");
            return "errou";
        }
    }
    
    /*
     * x = posicao
     */
    private static boolean setNewShip(int posicaoEixoX, int posicaoEixoY, int tamanho, boolean isHorizontal, int tipoNavio){
         	
        if(!isHorizontal){
        	//verifica se o tamanho existe no tabuleiro
        	if((posicaoEixoY+tamanho)<7){
	    		//verifica se a posicao escolhida esta livre
		    	for(int i = 0; i < tamanho; i++){
		    		
		        	if(board[posicaoEixoX][posicaoEixoY+i] != -1){
		        		return false;
		        	}
		        }
		    	for(int i = 0; i < tamanho; i++){
		    		board[posicaoEixoX][posicaoEixoY+i] = tipoNavio;
		    	}
        	}else return false;
        }
        else{
        	//verifica se o tamanho existe no tabuleiro
        	if((posicaoEixoX+tamanho)<7){
	    		//verifica se a posicao escolhida esta livre
		    	for(int i = 0; i < tamanho; i++){
		        	if(board[posicaoEixoX+i][posicaoEixoY] == -1){
		        		return false;
		        	}
		        }
		    	for(int i = 0; i < tamanho; i++){
		    		board[posicaoEixoX+i][posicaoEixoY] = tipoNavio;
		    	}
        	}else return false;
        }
        return true;
    }
    
    private static void setRandomSubmarino(){
    	Random randomize = new Random();
    	boolean tryAgain = false;
    	while(!tryAgain){
    		 int x = randomize.nextInt(7);
             int y = randomize.nextInt(7);
             boolean z = randomize.nextBoolean();
             tryAgain = setNewShip(x, y, 1, z, 1);
    	}
    }
    
    private static void setRandomCruzador(){
    	Random randomize = new Random();
    	boolean tryAgain = false;
    	while(!tryAgain){
    		 int x = randomize.nextInt(7);
             int y = randomize.nextInt(7);
             boolean z = randomize.nextBoolean();
             tryAgain = setNewShip(x, y, 2, z, 2);
    	}
    }
    
    private static void setRandomPortaAvioes(){
    	Random randomize = new Random();
    	boolean tryAgain = false;
    	while(!tryAgain){
    		 int x = randomize.nextInt(7);
             int y = randomize.nextInt(7);
             boolean z = randomize.nextBoolean();
             tryAgain = setNewShip(x, y, 3, z, 3);
    	}
    }
    
    private static boolean isGameOver(){
    
    
    	for(int i=0;i < 7;i++){
    		for(int j=0;j < 7;j++){
    			if(board[i][j] == 1 || board[i][j] == 2 || board[i][j] == 3){
    				return false;
    			}
    		}
    	}
    return true;
    }

    
    public static byte[] intsToBytes(int[] ints) {
        ByteBuffer bb = ByteBuffer.allocate(ints.length * 4);
        IntBuffer ib = bb.asIntBuffer();
        for (int i : ints) ib.put(i);
        return bb.array();
    }

    public static int[] bytesToInts(byte[] bytes) {
        int[] ints = new int[bytes.length / 4];
        ByteBuffer.wrap(bytes).asIntBuffer().get(ints);
        return ints;
    }
    
    private static void waitYourTurn(){
        System.out.println("Por favor aguarde a jogada do adversário...");
        System.out.println("");
    }
    
    private static void clearConsole(){
        String n = "\r\n";
        for (int i = 0; i < 50; i++) {
            n = n+"\r\n";
        }
        System.out.println(n);
    }
}

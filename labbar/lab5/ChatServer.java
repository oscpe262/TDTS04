import ChatApp.*;          // The package containing our stubs. 
import org.omg.CosNaming.*; // HelloServer will use the naming service. 
import org.omg.CosNaming.NamingContextPackage.*; // ..for exceptions. 
import org.omg.CORBA.*;     // All CORBA applications need these classes. 
import org.omg.PortableServer.*;   
import org.omg.PortableServer.POA;
import java.util.*;

/*
For each method Callbacking, add try-catch<client.remove> 
 */

class ChatImpl extends ChatPOA {
  private ORB orb;
  Map<String, ChatCallback> clients = new HashMap<String, ChatCallback>();
      Game game = new Game();
  
  public void setORB(ORB orb_val) {
    orb = orb_val;
  }
  
    public String join(ChatCallback objref, String nickname){    
	if(clients.containsKey(nickname)){
	    objref.callback("\u001b[31;1m" + nickname + " is already an active chatter\u001b[0m");
	    return "active";
	}
	for (ChatCallback callback : clients.values()) {
	    try {
		callback.callback("\u001b[33m" + nickname + " has joined!\u001b[0m"); // goes out to everyone
	    }
	    catch(Exception e){
		System.out.println("\u001b[31;1mLost connection to peer! \u001b[0m");
		for(Iterator<Map.Entry<String,ChatCallback>>it = clients.entrySet().iterator(); it.hasNext(); ){
		    Map.Entry<String, ChatCallback> entry = it.next();
		    it.remove();
		}
		
	    }
	}
    
	objref.callback("\u001b[36mWelcome " + nickname + "!\u001b[0m");
	clients.put(nickname, objref);
	return nickname;
    
    }

    public void ping(ChatCallback objref, String nickname) {
	if(!(clients.containsKey(nickname))){
	    clients.put(nickname, objref);
	    objref.callback("\u001b[31;6mSomething has gone wrong, reconnecting... \u001b[0m" );
	}
    }
  
    public void post(ChatCallback objref, String nickname, String msg){
	for (ChatCallback callback : clients.values()) {
	    try {
		callback.callback("\u001b[34;1m" + nickname + ":\u001b[0m" + msg);
	    }
	    catch(Exception e){
		System.out.println("\u001b[31;1mLost connection to peer! \u001b[0m");
		for(Iterator<Map.Entry<String,ChatCallback>>it = clients.entrySet().iterator(); it.hasNext(); ){
		    Map.Entry<String, ChatCallback> entry = it.next();
		    it.remove();
		}
	    }
	    
	}   
    }
  
    public void list(ChatCallback objref, String nickname){
	objref.callback("\u001b[36mList of registered users: \u001b[0m");
       
	for(String joinedNicks : clients.keySet()){
	    objref.callback(joinedNicks);
	}
    }

    public void leave(ChatCallback objref, String nickname){
	clients.remove(nickname); // remove post in hash
	for (ChatCallback callback : clients.values()) {
	    try {
		callback.callback("\u001b[33m" + nickname + " has left.\u001b[0m"); // broadcast message
	    }
	    catch(Exception e){
		System.out.println("\u001b[31;1mLost connection to peer! \u001b[0m");
		for(Iterator<Map.Entry<String,ChatCallback>>it = clients.entrySet().iterator(); it.hasNext(); ){
		    Map.Entry<String, ChatCallback> entry = it.next();
		    it.remove();
		}
	    }
	}
	
	objref.callback("Cheers " + nickname);
    }

    
    public void play(ChatCallback objref, String nickname, String color){
	game.join(objref, nickname, color);
    }

    public void put(ChatCallback objref, String nickname, String pos){
	game.put(objref, nickname, pos); 
    }

    public void leaveGame(String nickname){
	game.leave(nickname);
    }

    /*
    public voic pass(String nickname){
	game.pass(nickname);
    }

    public void reset() {
        game.reset();
    }

    */
}

public class ChatServer {
    public static void main(String args[]) {
	try { 
	    // create and initialize the ORB
	    ORB orb = ORB.init(args, null); 

	    // create servant (impl) and register it with the ORB
	    ChatImpl chatImpl = new ChatImpl();
	    chatImpl.setORB(orb); 

	    // get reference to rootpoa & activate the POAManager
	    POA rootpoa = 
		POAHelper.narrow(orb.resolve_initial_references("RootPOA"));  
	    rootpoa.the_POAManager().activate(); 

	    // get the root naming context
	    org.omg.CORBA.Object objRef = 
		orb.resolve_initial_references("NameService");
	    NamingContextExt ncRef = NamingContextExtHelper.narrow(objRef);

	    // obtain object reference from the servant (impl)
	    org.omg.CORBA.Object ref = 
		rootpoa.servant_to_reference(chatImpl);
	    Chat cref = ChatHelper.narrow(ref);

	    // bind the object reference in naming
	    String name = "Chat";
	    NameComponent path[] = ncRef.to_name(name);
	    ncRef.rebind(path, cref);

	    System.out.println("\u001b[32;1m\nChatServer ready and waiting ...\u001b[0m");
	    
	    // wait for invocations from clients
	    orb.run();
	}
	    
	catch(Exception e) {
	    System.err.println("\u001b[31;1mERROR : " + e + "\u001b[0m");
	    e.printStackTrace(System.out);
	}

	System.out.println("ChatServer Exiting ...");
    }

}

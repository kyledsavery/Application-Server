package appserver.comm;

import java.io.Serializable;

/**
 * Class [Message] Defines a generic Message that has a message type and content.
 * Instances of this class can be sent over a network, using object streams.
 * Message types are defined in MessageTypes
 * 
 * @author Dr.-Ing. Wolf-Dieter Otte
 */
public class Message implements MessageTypes, Serializable {

    // contains the type of message, types are defined in interface MessageTypes
    int type;
    
    // contains the content that is specific to a certain message type
    Object content;
    
    /* ID for this message, used to keep track of which jobs go to/from which
    satellites*/
    int messageId;

    public Message(int type, Object content) {
        this.type = type;
        this.content = content;
    }
    
    public Message(int type, Object content, int messageId){
        this.type = type;
        this.content = content;
        this.messageId = messageId;
    }

    public Message() {
    }
    
// getter and setter methods for message type
    public void setType(int type) {
        this.type = type;
    }

    // getter and setter methods for parameters
    public int getType() {
        return type;
    }
    
    public void setId(int messageId){
        this.messageId = messageId;
    }
    
    public int getId(){
        return messageId;
    }
    
    public void setContent(Object content) {
        this.content = content;
    }

    public Object getContent() {
        return content;
    }
}

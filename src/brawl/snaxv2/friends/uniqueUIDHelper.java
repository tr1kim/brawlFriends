package brawl.snaxv2.friends;

import java.util.UUID;

public class uniqueUIDHelper {
	public UUID formatFromInput(String uuid) throws IllegalArgumentException{
	    if(uuid == null) throw new IllegalArgumentException();
	    uuid = uuid.trim();
	    return uuid.length() == 32 ? fromTrimmed(uuid.replaceAll("-", "")) : UUID.fromString(uuid);
	}

	public UUID fromTrimmed(String trimmedUUID) throws IllegalArgumentException{
	    if(trimmedUUID == null) throw new IllegalArgumentException();
	    StringBuilder builder = new StringBuilder(trimmedUUID.trim());
	    /* Backwards adding to avoid index adjustments */
	    try {
	        builder.insert(20, "-");
	        builder.insert(16, "-");
	        builder.insert(12, "-");
	        builder.insert(8, "-");
	    } catch (StringIndexOutOfBoundsException e){
	        throw new IllegalArgumentException();
	    }
	 
	    return UUID.fromString(builder.toString());
	}
}

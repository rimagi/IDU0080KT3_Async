package ee.ttu.idu0080.raamatupood.types;

import java.io.Serializable;
import java.math.BigDecimal;

public class Toode implements Serializable{
	private static final long serialVersionUID = 1L;

	public Integer kood; 
	public String nimetus;
	public BigDecimal hind;
	
	public Toode(Integer kood, String nimetus, BigDecimal hind)
	{
		this.kood = kood;
		this.nimetus = nimetus;
		this.hind = hind; 
	}
	
}

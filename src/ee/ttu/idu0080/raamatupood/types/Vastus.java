package ee.ttu.idu0080.raamatupood.types;

import java.io.Serializable;
import java.math.BigDecimal;

public class Vastus implements Serializable{
	private static final long serialVersionUID = 1L;
	public boolean tulemus; 
	public BigDecimal koondHind;
	public String veaKirjeldus; 
	
	public Vastus()
	{
	}

	public void setError(String kirjeldus)
	{
		veaKirjeldus = kirjeldus; 
		tulemus = false;
	}
	
	public void setHind(double hind)
	{
		koondHind = new BigDecimal(hind);
		tulemus = true;
	}
}

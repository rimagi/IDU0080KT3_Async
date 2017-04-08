package ee.ttu.idu0080.raamatupood.types;

import java.io.Serializable;

public class TellimuseRida implements Serializable{
	private static final long serialVersionUID = 1L;

	public Toode toode;
	public Long kogus;
	
	public TellimuseRida(Toode toode, Long kogus)
	{
		this.toode  =toode; 
		this.kogus = kogus;
	}
}

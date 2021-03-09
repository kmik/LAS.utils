package runners;

public class AjaPilkonta {

	public static void main (String [ ] args) {
	
		String input = args[0];
		String outputlocation = args[1];
		//String size = args[2];
		//int koko = Integer.parseInt(size); 				
		
	Pilkonta uusiPilko = new Pilkonta();
	
	uusiPilko.Pilko(input,outputlocation);
	
}
}
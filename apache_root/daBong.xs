/***********************************************************************************************
 *	DaBong Application
 *
 *	Date			Action				Author			EMail
 *	december 4th 2010    	Created				Dan Marin		dan.marin@dabo.ro
 ***********************************************************************************************/

#unit DaBong_Application

#import xsdk/xapp.xs
#import xsdk/xgui.xs
#import xsdk/mediaman.xs
//#import xsdk/utils.xs

/*class Brick {
	int x;
	int y;
	
	Brick(int x1, int y1) {
		x = x1;
		y = y1;
	}
};*/

object<XApp> crtInstance;
object<Window> wMain;
object<Canvas> cnvMain;

/**************************************************
 * Colectie de variabile pentru Obiectul Pad
 **************************************************/
object<WebImage> imgPadLeft;
object<WebImage> imgPadCenter;
object<WebImage> imgPadRight;

float iPadx;
float iPady;

float fPadMinY;
float fPadMaxY;

int iPadSegments;

float iPadSize;

int iPadSpeed;
int bPadBounce;


int bMoovingRight;
int bMoovingLeft;

void PadPaint();
void ResetPad();
void RecalculatePadSize();

void PadStep();
/**************************************************
 * End Oject Pad
 **************************************************/

/**************************************************
 * Colectie de variabile pentru Obiectul Ball
 **************************************************/
object<WebImage> imgBall;

float iBallx;
float iBally;

float iBallVx;
float iBallVy;

float iBallSpeed;
float fBallAngle;

void BallStep();
void ResetBall();
/**************************************************
 * End Oject Ball
 **************************************************/


/**************************************************
 * Colectie de variabile pentru Obiectele Dummy
 * Cand o caramida se loveste de un obiect dummy isi schimba directia
 * in functie de tipul obiectului
 **************************************************/
int iNoDummies;

float[] iDummyX;
float[] iDummyY;

int[]   iDummyID;
// - 0 Schimba directia spre dreapta vx = 1 vy = 0
// - 1 Schimba directia spre stanga  vx = -1 vy = 0
// - 2 Schimba directia in sus 	     vx = 0 vy = -1
// - 3 Schimba directia in jos       vx = 0 vy = 1

/**************************************************
 * End Oject Ball
 **************************************************/

/**************************************************
 * Colectie de variabile pentru Obiectele Brick
 **************************************************/
int iLifes;
int iNoBricks;

object<WebImage> imgBrick;

float[] fBrickx;
float[] fBricky;

float[] fBrickH;
float[] fBrickW;

int[] bBrickVisible;

//variabile pentru caramizi ce se misca

float[] fBrickSpeedX;
float[] fBrickSpeedY;

void StepBricks();
void DrawBricks();
/**************************************************
 * End Oject Brick
 **************************************************/



/**************************************************
 * Colectie de variabile pentru Obiectele PowerUP
 **************************************************/
object<WebImage>[] imgPowerUP;

int iMaxPowerUPs;

float[] fPowerUPx;
float[] fPowerUPy;

int[]	iIDPowerUP;
int[]   iPowerUPVisible;

void PowerUPStep();

/**************************************************
 * End Oject PowerUP
 **************************************************/



void Paint(object<Timer> sender);
void keyPressed(object<VisualComponent> sender, object<KeyArgs> arg);
void keyReleased(object<VisualComponent> sender, object<KeyArgs> arg);
void mouseMoved(object<VisualComponent> sender, object<MouseArgs> arg);

string initApp(object<XApp> app)
{
	crtInstance = app;
	
	wMain = app.createWindow(new object<WindowDesc>("DaBong", 500, 500));
	if (wMain==null) return "Couldn't create window.";

	wMain.onKeyPressed = keyPressed;
	wMain.onKeyReleased = keyReleased;
	
	wMain.onMouseMoved = mouseMoved;

	cnvMain = new object<Canvas>(10, 10, 450, 400);
	wMain.addControl(cnvMain);
	
	wMain.show();
	
	/*************************
   	 *Se Creeaza Obiectul pad
 	 *************************/

    imgPadLeft = _mediaManager.requestImage("applications/DaBong/pad/pad_left.png");
	imgPadRight = _mediaManager.requestImage("applications/DaBong/pad/pad_right.png");
	imgPadCenter = _mediaManager.requestImage("applications/DaBong/pad/pad_middle.png");

	iLifes = 3;
	ResetPad();

	/*************************
   	 *End Obiect pad
 	 *************************/

	/*************************
   	 *Se Creeaza Obiectul pad
 	 *************************/

         imgBall = _mediaManager.requestImage("applications/DaBong/balls/ball18.png");
	 ResetBall();
	
	/*************************
   	 *End Obiect pad
 	 *************************/

	/*************************
   	 *Se Creeaza Obiectele Brick
 	 *************************/

        imgBrick = _mediaManager.requestImage("applications/DaBong/bricks/brickz_32.png");
	
	iNoBricks = 49;

	fBrickx = new float[iNoBricks];
	fBricky = new float[iNoBricks];
	fBrickH = new float[iNoBricks];
	fBrickW = new float[iNoBricks];

	bBrickVisible = new int[iNoBricks];

	fBrickSpeedX = new float[iNoBricks];
	fBrickSpeedY = new float[iNoBricks];


	for (int i=0;i<12;i++;)
		for (int j=0;j<4;j++;)
	{
		fBrickx[i+j*12] = 20+i*35;
		fBricky[i+j*12] = 20+j*35;
	
		fBrickH[i+j*12] = 30;
		fBrickW[i+j*12] = 30;
	
		bBrickVisible[i+j*12] = 1;

		fBrickSpeedX[i+j*12] = 0;
		fBrickSpeedY[i+j*12] = 0;
	}
	
	
	fBrickx[48] = 100;
	fBricky[48] = 180;
	fBrickH[48] = 30;
	fBrickW[48] = 30;
	bBrickVisible[48] = 1;
	
	fBrickSpeedX[48] = 1;
	fBrickSpeedY[48] = 0;

	/*************************
   	 *End Obiecte Brick
 	 *************************/

	/*************************
   	 *Se Creeaza Obiectele Dummy
 	 *************************/

	iNoDummies = 2;	

	iDummyX = new float[iNoDummies]; 
	iDummyY = new float[iNoDummies];
	iDummyID = new int[iNoDummies];

	iDummyX[0] = 60;
	iDummyY[0] = 180;
	iDummyID[0] = 0;

	iDummyX[1] = 340;
	iDummyY[1] = 180;	
	iDummyID[1] = 1;

	/*************************
   	 *End Obiecte Dummy
 	 *************************/

	/*************************
   	 *Se Creeaza Obiectele PowerUP
 	 *************************/
	imgPowerUP = new object<WebImage>[2];
	imgPowerUP[0] = _mediaManager.requestImage("applications/DaBong/PowerUps/enlarge.png");
	imgPowerUP[1] = _mediaManager.requestImage("applications/DaBong/PowerUps/shrink.png");

	iMaxPowerUPs = 10;

	fPowerUPx = new float[iMaxPowerUPs];
	fPowerUPy = new float[iMaxPowerUPs];

	iIDPowerUP = new int[iMaxPowerUPs];	
	iPowerUPVisible = new int[iMaxPowerUPs];
	
	for (int i=0;i<10;i++;) 
		iPowerUPVisible[i] = 0;	
		
	/*************************
   	 *End Obiecte PowerUP
 	 *************************/

	object<Timer> RenderTimer = new object<Timer>(10);
	wMain.addControl(RenderTimer);
	RenderTimer.setEnabled(true);
	RenderTimer.onTick = Paint;

	return null;
}

void mouseMoved(object<VisualComponent> sender, object<MouseArgs> arg)
{
	iPadx = iPadx;
}

void ResetPad()
{
	iPadx = 200;
	iPady = 370;
	iPadSegments = 1;
	RecalculatePadSize();
	iPadSpeed = 2;
	bMoovingLeft = 0;
	bMoovingRight = 0;
	fPadMaxY = iPady + 5;
	fPadMinY = iPady;
	bPadBounce = 0;
}

void RecalculatePadSize()
{
	iPadSize = 40+(iPadSegments*20);
}

void PadPaint()
{
	cnvMain.drawImage(imgPadLeft, Math.round(iPadx), Math.round(iPady));
	for (int i=0;i<iPadSegments;i++;)
		cnvMain.drawImage(imgPadCenter,Math.round(iPadx)+20*(i+1),Math.round(iPady));
	cnvMain.drawImage(imgPadRight,Math.round(iPadx)+20*(iPadSegments+1), Math.round(iPady));
}

void ResetBall()
{
	iBallx = 200;
	iBally = 280;
	
	iBallSpeed = 2;
	fBallAngle = 90 * Math.PI / 180;

	iBallVx = Math.cos(fBallAngle)*iBallSpeed;
	iBallVy = Math.sin(fBallAngle)*iBallSpeed;
}

void StepBricks()
{
	for (int i=0;i<iNoBricks;i++;)
	{
		if (bBrickVisible[i] == 1) 
		{
			if (fBrickSpeedX[i] != 0 || fBrickSpeedY[i] != 0) 
			{
				fBrickx[i] += fBrickSpeedX[i];
				fBricky[i] += fBrickSpeedY[i];
		
				for (int j=0;j<iNoDummies;j++;)
				{
					if (fBrickx[i]+fBrickW[i] >= iDummyX[j] && fBrickx[i] <= iDummyX[j]+30 && fBricky[i]+fBrickH[i] >= iDummyY[j] && fBricky[i] <= iDummyY[j]+30)
					{
						if (iDummyID[j] == 0) 
						{
							fBrickSpeedX[i] = 1;
							fBrickSpeedY[i] = 0;
						}
						if (iDummyID[j] == 1) 
						{
							fBrickSpeedX[i] = -1;
							fBrickSpeedY[i] = 0;
						}
						if (iDummyID[j] == 2) 
						{
							fBrickSpeedX[i] = 0;
							fBrickSpeedY[i] = -1;
						}
						if (iDummyID[j] == 3) 
						{
							fBrickSpeedX[i] = 0;
							fBrickSpeedY[i] = 1;
						}
					}
				}
			}
		}
	}
}


void BallStep()
{
	float iCenterBx;
	float iCenterBy;

	float iCenterPx;

	iCenterBx = iBallx + 10.0;
	iCenterBy = iBally + 10.0;

	iCenterPx = iPadx + iPadSize / 2;

	float dx;
	float dy;
	
	/**************************************************
	* Se calculeaza coliziunea cu pad'ul
	*************************************************/

	if (iBallx+20 > iPadx && iBallx < iPadx+iPadSize && iBally+20+iBallVy > iPady)  
	{
		dx = iCenterPx - iCenterBx;
	        fBallAngle = (270-dx * 90.0/iPadSize) * Math.PI / 180;

		iBallVx = Math.cos(fBallAngle)*iBallSpeed;
       		iBallVy = Math.sin(fBallAngle)*iBallSpeed;		
		bPadBounce = 1;
	}
	
	/**************************************************
	* Se calculeaza coliziunea cu Brick'urile
	*************************************************/
	
	for (int i=0;i<iNoBricks;i++;) 
	{
		if (iBally < fBricky[i]+fBrickH[i] && iBally+20 > fBricky[i] && bBrickVisible[i] == 1) 
		    if (iBallx+iBallVx < fBrickx[i]+fBrickW[i] && iBallx+20+iBallVx>fBrickx[i]) 
		    {
			if (iBallVx < 0) 
			{
				dy = fBricky[i] + fBrickH[i]/2 - iCenterBy;
		        	fBallAngle = (0-dy * 90.0/30.0) * Math.PI / 180;
				iBallVx = Math.cos(fBallAngle)*iBallSpeed;
	       			iBallVy = Math.sin(fBallAngle)*iBallSpeed;
				bBrickVisible[i] = 0;
			} 
			else
			{
				dy = fBricky[i] + fBrickH[i]/2 - iCenterBy;
		        	fBallAngle = (180+dy * 90.0/30.0) * Math.PI / 180;
				iBallVx = Math.cos(fBallAngle)*iBallSpeed;
	       			iBallVy = Math.sin(fBallAngle)*iBallSpeed;
				bBrickVisible[i] = 0;	
			}
		        /**
		        * sansa de 20% de a aparea un powerup :D
		        */
			if (Math.rand(0,100) < 20) 
			{
				for (int j=0;j<iMaxPowerUPs;j++;)
				{
					if (iPowerUPVisible[j] == 0) 
					{
						iPowerUPVisible[j] = 1;
						fPowerUPy[j] = fBricky[i]+20;
						fPowerUPx[j] = fBrickx[i]+5;
						iIDPowerUP[j] = Math.round(Math.rand(0.4,1.4));
						break;
					}
				}
			}
		        
		    }
		if (iBallx < fBrickx[i]+fBrickW[i] && iBallx+20>fBrickx[i] && bBrickVisible[i] == 1)
		    if (iBally+iBallVy < fBricky[i]+fBrickH[i] && iBally+20+iBallVy > fBricky[i]) 
                    {
			if (iBallVy < 0) 
			{
				dx = fBrickx[i] + fBrickW[i]/2 - iCenterBx;
		        	fBallAngle = (90+dx * 90.0/30.0) * Math.PI / 180;
				iBallVx = Math.cos(fBallAngle)*iBallSpeed;
	       			iBallVy = Math.sin(fBallAngle)*iBallSpeed;		
				bBrickVisible[i] = 0;
			} 
			else
			{
				dx = fBrickx[i] + fBrickW[i]/2 - iCenterBx;
		        	fBallAngle = (270-dx * 90.0/30.0) * Math.PI / 180;
				iBallVx = Math.cos(fBallAngle)*iBallSpeed;
	       			iBallVy = Math.sin(fBallAngle)*iBallSpeed;		
				bBrickVisible[i] = 0;
			}
		        /**
		        * sansa de 20% de a aparea un powerup :D
		        */
			if (Math.rand(0,100) < 20) 
			{
				for (int j=0;j<iMaxPowerUPs;j++;)
				{
					if (iPowerUPVisible[j] == 0) 
					{
						iPowerUPVisible[j] = 1;
						fPowerUPy[j] = fBricky[i]+20;
						fPowerUPx[j] = fBrickx[i]+5;
						iIDPowerUP[j] = Math.floor(Math.rand(0.4,1.4));
						break;
					}
				}
			}
		    }
	}	
	/**************************************************************************
	* Miscarea simpla a bilei: daca se loveste de pereti isi schimba directia
	*************************************************************************/
	if (iBallx+iBallVx <= 0 || iBallx+20+iBallVx >= 450) {
		iBallVx *= -1;
	}

	if (iBally+iBallVy <= 0) {
		iBallVy *= -1;
	}

	/**************************
	* Se deplaseaza bila
	*************************/
	
	iBallx+=iBallVx;
	iBally+=iBallVy;

	/**************************
	* Daca s-a pierdut bila se scade o viata
	*************************/
	
	if (iBally+20 >= 400) 
	{
		ResetBall();
		iLifes --;
		ResetPad();
	}
	
}


void PadStep()
{
	if (bPadBounce == 1) {
		iPady +=0.5;
		if (iPady >= fPadMaxY)
			bPadBounce = -1;
	} else if (bPadBounce == -1) {
		iPady -=0.5;
		if (iPady <= fPadMinY)
			bPadBounce = 0;
	
	}
	if (bMoovingLeft == 1 && iPadx > 0)
		iPadx -=iPadSpeed;		
	if (bMoovingRight == 1 && iPadx < 450-iPadSize)
		iPadx +=iPadSpeed;
}



void DrawBricks()
{
	for (int i=0;i<iNoBricks;i++;) 
	{
		if (bBrickVisible[i] == 1) 
			cnvMain.drawImage(imgBrick,Math.round(fBrickx[i]),Math.round(fBricky[i]));
	}
}

void DrawPowerUPs()
{
	for (int i=0;i<iMaxPowerUPs;i++;)
	{
		if (iPowerUPVisible[i] == 1) 
		    {
			cnvMain.drawImage(imgPowerUP[iIDPowerUP[i]], Math.round(fPowerUPx[i]),Math.round(fPowerUPy[i]));
		    }
	}
}

void PowerUPStep()
{
	for (int i=0;i<iMaxPowerUPs;i++;)
	{
		if (iPowerUPVisible[i] == 1) 
		    {
			fPowerUPy[i]++;
			/*****
			*Se verifica daca s-a ajuns jos
			*/
			if (fPowerUPy[i]+20 >= 400) 
			{
				iPowerUPVisible[i] = 0;
			}
			if (fPowerUPx[i]+20 > iPadx && fPowerUPx[i] < iPadx+iPadSize && fPowerUPy[i]+20 > iPady)  
			{
				iPowerUPVisible[i] = 0;
				if (iIDPowerUP[i] == 0) 
				{
					iPadSegments++;
					RecalculatePadSize();
				}
				if (iIDPowerUP[i] == 1) 
				{
					if (iPadSegments >= 1) 
					{
						iPadSegments--;
						RecalculatePadSize();
					}
				}
	
			}
		    }
	}
}

void Paint(object<Timer> sender)
{
	PadStep();
	BallStep();
	PowerUPStep();
	StepBricks();
	cnvMain.clear(Color.transparent);

	cnvMain.drawImage(imgBall,Math.round(iBallx),Math.round(iBally));
	DrawPowerUPs();
	PadPaint();
	DrawBricks();
		
	cnvMain.update();
}


void keyPressed(object<VisualComponent> sender, object<KeyArgs> arg)
{
	if (arg.keyCode == KeyArgs.KEY_LEFT)
		bMoovingLeft = 1;		
	if (arg.keyCode == KeyArgs.KEY_RIGHT && iPadx < 450-iPadSize)
		bMoovingRight = 1;
	if (arg.keyCode == KeyArgs.KEY_UP) {
		iPadSegments++;
		RecalculatePadSize();
	}
	if (arg.keyCode == KeyArgs.KEY_DOWN) {
		bPadBounce = 1;
		//iPadSegments--;
		//RecalculatePadSize();
	}
}

void keyReleased(object<VisualComponent> sender, object<KeyArgs> arg)
{
	if (arg.keyCode == KeyArgs.KEY_LEFT)
		bMoovingLeft = 0;
		
	if (arg.keyCode == KeyArgs.KEY_RIGHT)
		bMoovingRight = 0;
}

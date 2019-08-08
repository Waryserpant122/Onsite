#include <stdio.h>
#include <string.h>
#include <math.h>
#include <stdlib.h>


int wincheck(int p,int q,int b)
{
    int c=1,n=1000000;
    while(n>0)
    {
      if(p==q)
        q=q-p;
      else if(p<q)
       q-=p;
       else
        q+=q;
        
      n--;
        
      if(q==0)
        {
            c=1;
            break;
		}
		
	   else if(q==b)
	    {
	    	c=0;
	    	break;
		}
     
    }
     return c;
  /*  if(q==0)
        c=1;
    else if(q==b)
        c=0;*/
    
}

int main() {

    /* Enter your code here. Read input from STDIN. Print output to STDOUT */    
    int t,i;
    int p,q;
    scanf("%d",&t);
    int a[t];
    for(i=0;i<t;i++)
    {
        scanf("%d %d",&p,&q);
        int b=q;
        if(p<=q)
        q=q-p;
    else
        q+=q;
       
        a[i]=wincheck(p,q,b);
         printf("%d",a[i]);
       // printf("%d",wincheck(p,q,b));
    }
   /* for(i=0;i<t;i++)
    {
        if(a[i]==1)
            printf("Yes\n");
        else
            printf("No\n");
    }*/
}


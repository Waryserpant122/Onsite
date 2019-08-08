#include <stdio.h>
#include <string.h>
#include <math.h>
#include <stdlib.h>
void doop(int p,int q)
{
   //long a,c,n;
    if(p<=q)
        q=q-p;
    else
        q+=q;
}

int wincheck(int p,int q,int b)
{
    doop(p,q);
    int c=0;
    while((q!=0)||(q!=b))
    {
        doop(p,q);
        
    }
    
    if(q==0)
        return 1;
    else
        return 0;
    
}

int main() {

    /* Enter your code here. Read input from STDIN. Print output to STDOUT */    
    int t,i;
    int p,q,b;
    scanf("%d",&t);
    int a[t];
    for(i=0;i<t;i++)
    {
        scanf("%d %d",&p,&q);
        b=q;
       a[i]=wincheck(p,q,b);
    }
    for(i=0;i<t;i++)
    {
        if(a[i]==1)
            printf("Yes\n");
        else
            printf("No\n");
    }
}


#include <stdio.h>
#include "wich.h"

double f(int x);

double f(int x)
{
    double y;
    y = 1.0;
    return (x + -y);

}


int main(int ____c, char *____v[])
{
	setup_error_handlers();
	double z;
	z = f(2);
	if ((z == 0)) {
	    print_string(String_new("z==0"));
	}
	else {
	    print_string(String_new("z!=0"));
	}
	return 0;
}


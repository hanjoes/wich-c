#include <stdio.h>
#include "wich.h"

Vector * x;

Vector * foo();

Vector * foo()
{
	Vector * y;
	y = Vector_new((double []){1,2,3,4,5}, 5);
	REF(y);
	DEREF(y);
	return y;
}

int main(int argc, char *argv[])
{
	x = foo();
	Vector * tmp1;
	print_vector(tmp1=foo());
	DEREF(x);
	DEREF(tmp1);
	return 0;
}

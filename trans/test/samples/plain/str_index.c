#include <stdio.h>
#include "wich.h"
void f();

void
f()
{
    String *x;

    x = String_add(String_new("cat"), String_new("dog"));
    print_string(x);
    print_string(String_add(String_from_char(x->str[(1) - 1]), String_from_char(x->str[(3) - 1])));
}

int
main(int argc, char *argv[])
{
	setup_error_handlers();
    f();
    return 0;
}

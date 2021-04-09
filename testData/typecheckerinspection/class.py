

class A:
    def a(self, b: str):
        pass

A().a(<warning descr="Expected type 'str', got 'int' instead">int(123)</warning>)


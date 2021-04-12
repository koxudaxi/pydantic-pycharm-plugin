
def conlist(item_type, *, min_items = None, max_items = None) Type[List[T]]:
    pass

def constr(
        *,
        strip_whitespace: bool = False,
        to_lower: bool = False,
        strict: bool = False,
        min_length: int = None,
        max_length: int = None,
        curtail_length: int = None,
        regex: str = None,
) -> Type[str]:
    pass
class Code_block():
    def __init__(self, code):
        self.code = TextMobject(*code)

    def build(self):
        return self.code.arrange(DOWN, aligned_edge=LEFT)

    def get_line_at(self, line_number):
        return self.code[line_number - 1]
